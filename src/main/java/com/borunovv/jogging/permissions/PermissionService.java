package com.borunovv.jogging.permissions;

import com.borunovv.core.service.AbstractService;
import com.borunovv.core.util.Assert;
import com.borunovv.jogging.users.model.Rights;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PermissionService extends AbstractService {
    private static final Map<Rights, List<Permission>> permissions = new LinkedHashMap<>();

    static {
        // What allowed to 'USER'
        allow(Rights.USER).canDo(Action.ANY_ACTION).on(Subject.ACCOUNT, Subject.TIMINGS).ownedBySelf();
        allow(Rights.USER).canDo(Action.READ).on(Subject.ACCOUNT_RIGHTS).ownedBySelf();

        // What allowed to 'MANAGER'
        allow(Rights.MANAGER).canDo(Action.ANY_ACTION).on(Subject.ACCOUNT, Subject.TIMINGS).ownedBySelf();
        allow(Rights.MANAGER).canDo(Action.ANY_ACTION).on(Subject.ACCOUNT, Subject.TIMINGS).ownedBy(Rights.USER);
        allow(Rights.MANAGER).canDo(Action.READ).on(Subject.ACCOUNT_RIGHTS).ownedBySelf();
        allow(Rights.MANAGER).canDo(Action.READ).on(Subject.ACCOUNT_RIGHTS).ownedBy(Rights.USER);

        // What allowed to 'ADMIN'
        allow(Rights.ADMIN).canDo(Action.ANY_ACTION).on(Subject.ANY_SUBJECT).ownedBySelf();
        allow(Rights.ADMIN).canDo(Action.ANY_ACTION).on(Subject.ANY_SUBJECT).ownedBy(Rights.USER);
        allow(Rights.ADMIN).canDo(Action.ANY_ACTION).on(Subject.ANY_SUBJECT).ownedBy(Rights.MANAGER);
        allow(Rights.ADMIN).canDo(Action.ANY_ACTION).on(Subject.ANY_SUBJECT).ownedBy(Rights.ADMIN);
    }

    private static Permission allow(Rights whom) {
       if (!permissions.containsKey(whom)) {
           permissions.put(whom, new ArrayList<>());
       }
        Permission perm = Permission.the(whom);
        permissions.get(whom).add(perm);
        return perm;
    }

    public void ensureHasPermission(Rights actor, Action action, Subject subject, Rights subjectOwner) {
        Assert.isTrue(hasPermission(actor, action, subject, subjectOwner), "Not enough rights.");
    }

    public boolean hasPermission(Rights actor, Action action, Subject subject, Rights subjectOwner) {
        List<Permission> actorPermissions = permissions.get(actor);
        Assert.notNull(actorPermissions, "Error: there are no one permission for '" + actor + "'");
        for (Permission p : actorPermissions) {
            if (p.matches(actor, action, subject, subjectOwner)) {
                return true;
            }
        }
        return false;
    }
}
