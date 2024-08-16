const Handlebars = require('handlebars');
const autocomplete = require('./autocomplete');

Handlebars.registerHelper('ifEquals', function(arg1, arg2, options) {
    return (arg1 == arg2) ? options.fn(this) : options.inverse(this);
});

export function init(roles, people, terms) {
    sortAll(roles);
    registerTabEvents(roles, terms, people);
    switchTab(roles, terms, people, 'Individual');
}

function registerTabEvents(roles, terms, people) {
    const tabs = document.getElementsByClassName('roles-tab');
    for (const tab of tabs) {
        const roleType = tab.getAttribute('data-type');
        tab.addEventListener('click', () => {
            switchTab(roles, terms, people, roleType);
        });
    }
}

function switchTab(roles, terms, people, roleType) {
    const tabs = document.getElementsByClassName('roles-tab');
    for (const tab of tabs) {
        if (tab.getAttribute('data-type') === roleType) {
            tab.classList.add('active');
        } else {
            tab.classList.remove('active');
        }
    }
    const filteredRoles = roles.filter(r => r.type === roleType);
    renderIndex(filteredRoles, terms, people, roleType);
}

function renderIndex(roles, terms, people, roleType) {
    const container = document.getElementById('roles-index-container');
    if (!roles || !roles.length) {
        container.innerHTML = '<div style="padding:15px;">No roles have been created yet with this type.</div>';
        return;
    }
    const template = Handlebars.compile(document.getElementById('roles-index-template').innerHTML);
    container.innerHTML = template({
        chairHeader: getChairHeader(roleType, terms),
        hasChairs: roles.some(r => getMembers(r, 'Chair').length > 0),
        hasBackups: roles.some(r => getMembers(r, 'Backup').length > 0),
        hasMembers: roles.some(r => getMembers(r, 'Member').length > 0),
        hasEligibilities: roles.some(r => r.eligibility !== 'Anyone'),
        hasNotes: roles.some(r => !!r.notes),
        hasDescriptions: roles.some(r => !!r.description),
        roles: roles.map(role => {
            return {
                id: role.id,
                name: role.name,
                chairs: formatMemberList(role, 'Chair'),
                backups: formatMemberList(role, 'Backup'),
                members: formatMemberList(role, 'Member'),
                eligibility: formatEligibility(role.eligibility),
                notes: role.notes,
                description: role.description
            };
        })
    });
    const editButtons = document.getElementsByClassName('roles-edit-button');
    for (const editButton of editButtons) {
        editButton.addEventListener('click', () => {
            const roleId = editButton.getAttribute('data-id');
            const role = roles.filter(r => r.id == roleId)[0];
            renderEditor(role, people);
        });
    }
    sorttable.makeSortable(container.querySelector('table'));
}

function renderEditor(role, people) {
    document.querySelector('.roles-editor').style.display = 'block';
    const table = document.querySelector('.roles-edit-table');
    const generalTemplate = Handlebars.compile(document.getElementById('roles-editor-general-template').innerHTML);
    const generalHtml = generalTemplate({
        name: role.name,
        type: role.type,
        eligibility: role.eligibility,
        notes: role.notes,
        description: role.description
    });
    table.innerHTML = generalHtml;
    table.innerHTML += getEditorSpecialHtml(role);

    const getPeopleResults = renderEditorPeople(role, people);
    registerEditorEvents(role, getPeopleResults);
}

function getEditorSpecialHtml(role) {
    if (role.type === 'Individual') {
        const template = Handlebars.compile(document.getElementById('roles-editor-individual-template').innerHTML);
        return template({
            chairs: getMembers(role, 'Chair'),
            backups: getMembers(role, 'Backup')
        });
    } else {
        const template = Handlebars.compile(document.getElementById('roles-editor-group-template').innerHTML);
        return template({
            chairs: getMembers(role, 'Chair'),
            members: getMembers(role, 'Member')
        });
    }
}

function renderEditorPeople(role, people) {
    const getResults = {
        chairs: () => [],
        backups: () => [],
        members: () => []
    };
    const chairsContainer = document.getElementById('roles-editor-chairs');
    const backupsContainer = document.getElementById('roles-editor-backups');
    const membersContainer = document.getElementById('roles-editor-members');
    const opts = {
        multi: true,
        allowPlainText: true,
        autoAdvance: true,
        textFieldSize: 14,
        textFieldClass: 'form-control'
    };
    if (chairsContainer) {
        const startingValues = formatMembersForAutocomplete(getMembers(role, 'Chair'));
        getResults.chairs = autocomplete.registerAutocomplete(chairsContainer, people, startingValues, opts);
    }
    if (backupsContainer) {
        const startingValues = formatMembersForAutocomplete(getMembers(role, 'Backup'));
        getResults.backups = autocomplete.registerAutocomplete(backupsContainer, people, startingValues, opts);
    }
    if (membersContainer) {
        const startingValues = formatMembersForAutocomplete(getMembers(role, 'Member'));
        getResults.members = autocomplete.registerAutocomplete(membersContainer, people, startingValues, opts);
    }
    return getResults;
}

function registerEditorEvents(role, getPeopleResults) {
    const buttonsContainer = document.getElementById('roles-editor-buttons');
    const template = Handlebars.compile(document.getElementById('roles-editor-buttons-template').innerHTML);
    buttonsContainer.innerHTML = template({});
    const submitButton = document.getElementById('roles-editor-submit');
    const cancelButton = document.getElementById('roles-editor-cancel');
    submitButton.addEventListener('click', () => {
        saveRole(role.id, getPeopleResults);
        updateIndex(role);
        closeEditor();
    });
    cancelButton.addEventListener('click', () => {
        closeEditor();
    });
}

function saveRole(id, getPeopleResults) {
    const roleJson = JSON.stringify({
        eligibility: document.getElementById('roles-editor-eligibility').value,
        name: document.getElementById('roles-editor-name').value,
        notes: document.getElementById('roles-editor-notes').value,
        description: document.getElementById('roles-editor-description').value,
        chairs: getPeopleResults.chairs(),
        backups: getPeopleResults.backups(),
        members: getPeopleResults.members()
    });
    const url = `/roles/updateRole/${id}?roleJson=${roleJson}`;
    fetch(url, { method: 'POST' });
}

function updateIndex(role) {

}

function closeEditor() {
    document.querySelector('.roles-editor').style.display = 'none';
}

function getChairHeader(roleType, terms) {
    if (roleType === 'Individual') {
        return terms.individual;
    }
    return 'Chair';
}

function getMembers(role, memberType) {
    if (!role.records || !role.records.length) {
        return [];
    }
    const lastRecord = role.records[role.records.length - 1];
    if (!lastRecord.members) {
        return [];
    }
    return lastRecord.members.filter(m => m.type === memberType);
}

function formatMemberList(role, memberType) {
    const members = getMembers(role, memberType);
    return members.map(m => m.personName).filter(m => !!m).join(', ');
}

function formatMembersForAutocomplete(members) {
    return members.map(m => {
        return {
            id: m.personId,
            label: m.personName
        };
    });
}

function formatEligibility(eligibility) {
    if (eligibility === 'StaffOnly') {
        return 'Staff Only';
    }
    if (eligibility === 'StudentOnly') {
        return 'Student Only';
    }
    return '';
}

function sortAll(roles) {
    sortRoles(roles);
    for (const role of roles) {
        if (role.records) {
            sortRecords(role.records);
            for (const record of role.records) {
                if (record.members) {
                    sortMembers(record.members);
                }
            }
        }
    }
}

function sortRoles(roles) {
    sortAlphabetically(roles, r => r.name);
}

function sortRecords(records) {
    // sorts chronologically
    records.sort((a, b) => sortFunction(a.dateCreated, b.dateCreated));
    function sortFunction(a, b) {
        // TODO IMPLEMENT
        return 0;
    }
}

function sortMembers(members) {
    sortAlphabetically(members, m => m.personName);
}

function sortAlphabetically(arr, nameFn) {
    arr.sort((a, b) => {
        const _a = nameFn(a).toLowerCase();
        const _b = nameFn(b).toLowerCase();
        if (_a > _b) return 1;
        if (_a < _b) return -1;
        return 0;
    });
}
