package models;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.Where;
import com.fasterxml.jackson.annotation.JsonIgnore;
import controllers.Utils;
import play.data.Form;

import javax.persistence.*;
import java.util.List;

@Entity
public class Section extends Model {
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "section_id_seq")
    public Integer id;

    public String title = "";
	public String num = "";

    @OneToMany(mappedBy="section")
    @OrderBy("num ASC")
	@Where(clause = "${ta}.deleted = false")
    @JsonIgnore
    public List<Entry> entries;

    @ManyToOne()
    public Chapter chapter;

	public Boolean deleted;

    public static Finder<Integer,Section> find = new Finder<>(
			Section.class
	);

    public static Section findById(int id) {
        return find.where().eq("chapter.organization", Organization.getByHost())
            .eq("id", id).findUnique();
    }

    public String getNumber() {
        return chapter.num + num;
    }

	public void updateFromForm(Form<Section> form) {
		title = form.field("title").getValue().get();
		num = form.field("num").getValue().get();
		chapter = Chapter.find.byId(Integer.parseInt(form.field("chapter.id").getValue().get()));
		deleted = Utils.getBooleanFromFormValue(form.field("deleted"));
		save();
	}

	public static Section create(Form<Section> form) {
		Section result = form.get();
		result.updateFromForm(form);
		result.save();
		return result;
	}
}
