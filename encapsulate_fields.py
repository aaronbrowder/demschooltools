import os
import re
import sys


def to_camel_case(snake_name: str):
    result = []
    last_was_underscore = False
    for c in snake_name:
        if c == '_':
            last_was_underscore = True
        elif last_was_underscore:
            last_was_underscore = False
            result.append(c.upper())
        else:
            result.append(c)
    return ''.join(result)


def generate_migration(table_name: str, replacements: dict[str, str]):
    ev_path = 'conf/evolutions/default'
    file_count = len(os.listdir(ev_path))

    ups = []
    downs = []

    for old_name, new_name in replacements.items():
        if old_name == new_name:
            continue
        ups.append(f'alter table {table_name} rename column {old_name} to {new_name};')
        downs.append(f'alter table {table_name} rename column {new_name} to {old_name};')

    with open(f'{ev_path}/{file_count+1}.sql', 'w') as f:
        f.write('# --- !Ups     \n\n')
        f.write('\n'.join(ups))
        f.write('\n\n# --- !Downs     \n\n')
        f.write('\n'.join(downs))


def encapsulate_fields(files: dict[str, str], path_to_model):
    assert path_to_model.endswith('.java')
    content = files[path_to_model]
    assert '@Entity' in content

    replacements = {}

    out_lines = []
    for line in content.splitlines():
        match = re.match(r'    public (\w+) (\w+)( ?= ?.*)?;', line)
        if not match:
            if line.strip() == '@Entity':
                out_lines.append('''\
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter''')
            out_lines.append(line)
            continue

        old_var_name = match.group(2)
        new_var_name = to_camel_case(old_var_name)
        replacements[old_var_name] = new_var_name

        out_lines.append(f'    private {match.group(1)} {new_var_name}{match.group(3) or ""};')

    files[path_to_model] = '\n'.join(out_lines)
    # generate_migration(get_table_name_from_path(path_to_model), replacements)


    for path, content in files.items():
        new_content = content
        for old_var_name, new_var_name in replacements.items():
            if old_var_name in {'id', 'name'}:
                continue

            if old_var_name not in content:
                continue

            capital_name = new_var_name[0].upper() + new_var_name[1:]
            getter = 'get' + capital_name
            setter = 'set' + capital_name

            if path != path_to_model and (path.endswith('.java') or path.endswith('.scala.html')):
                # .foo_bar = value; --> .setFooBar(value);
                new_content = re.sub(rf'\.{old_var_name} ?= ?(.*);',
                                     rf'.{setter}(\1);', new_content)
                # .foo_bar --> .getFooBar();
                new_content = re.sub(rf'\.{old_var_name}\b',
                                     f'.{getter}()', new_content)

            if old_var_name != new_var_name:
                # "foo_bar" --> "fooBar"
                new_content = re.sub(rf'\b{old_var_name}\b', new_var_name, new_content)

        files[path] = new_content



def get_table_name_from_path(path_to_model):
    return os.path.basename(path_to_model).split('.')[0].lower()


def load_files_one_path(result, path):
    for root, unused_dirs, files in os.walk(path):
        for filename in files:
            if os.path.splitext(filename)[1] in {'.java', '.html', '.js', '.conf'}:
                path = os.path.join(root, filename)
                with open(path) as f:
                    result[path] = f.read()


def load_files() -> dict[str, str]:
    result = {}
    load_files_one_path(result, 'app')
    load_files_one_path(result, 'conf')
    load_files_one_path(result, 'modelsLibrary/app')
    return result


def write_files(files):
    for path, content in files.items():
        with open(path, 'w') as f:
            f.write(content)


def main():
    files = load_files()
    for arg in sys.argv[1:]:
        encapsulate_fields(files, arg)

    write_files(files)


if __name__ == '__main__':
    main()
