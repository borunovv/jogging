package com.borunovv.jogging.permissions;

import com.borunovv.jogging.users.model.Rights;

public class Permission {
    private Rights actor;
    private Action[] actions;
    private Subject[] subjects;
    private Rights subjectOwner = Rights.Self;

    private Permission() {
    }

    public static Permission the(Rights actor) {
        Permission p = new Permission();
        p.actor = actor;
        return p;
    }

    public Permission canDo(Action ... actions) {
        this.actions = actions;
        return this;
    }

    public Permission on(Subject ... subjects) {
        this.subjects = subjects;
        return this;
    }

    public Permission ownedBy(Rights subjectOwner) {
        this.subjectOwner = subjectOwner;
        return this;
    }

    public Permission ownedBySelf() {
        this.subjectOwner = Rights.Self;
        return this;
    }

    public boolean matches(Rights actor, Action action, Subject subject, Rights subjectOwner) {
        if (this.actor != actor) return false;
        if (actions == null) return false;
        if (subjects == null) return false;
        if (this.subjectOwner != subjectOwner) return false;

        boolean hasAction = false;
        for (int i = 0; i < actions.length; ++i) {
            if (actions[i] == action || actions[i] == Action.ANY_ACTION) {
                hasAction = true;
                break;
            }
        }
        if (! hasAction) return false;

        boolean hasSubject = false;
        for (int i = 0; i < subjects.length; ++i) {
            if (subjects[i] == subject || subjects[i] == Subject.ANY_SUBJECT) {
                hasSubject = true;
                break;
            }
        }
        if (! hasSubject) return false;
        return true;
    }
}
