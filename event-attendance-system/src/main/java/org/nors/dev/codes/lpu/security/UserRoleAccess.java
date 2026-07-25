package org.nors.dev.codes.lpu.security;

import java.util.EnumSet;
import java.util.Set;
import org.nors.dev.codes.lpu.model.Role;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Which user accounts each role may list, create, edit, or deactivate. */
public final class UserRoleAccess {

    private UserRoleAccess() {
    }

    public static Set<Role> manageableRoles(Role acting) {
        return switch (acting) {
            case SUPERADMIN -> EnumSet.allOf(Role.class);
            case OSAS, EVENT_MAKER -> Set.of();
        };
    }

    public static boolean canManage(Role acting, Role target) {
        return manageableRoles(acting).contains(target);
    }

    public static void ensureCanManage(Role acting, Role target) {
        if (!canManage(acting, target)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot manage users with role " + target.name()
            );
        }
    }

    public static boolean hidesAttendanceIdentifiers(Role role) {
        return role == Role.EVENT_MAKER;
    }

    public static boolean canEditEvents(Role role) {
        return role == Role.SUPERADMIN || role == Role.OSAS;
    }

    public static boolean canToggleEventActive(Role role) {
        return role == Role.SUPERADMIN || role == Role.OSAS;
    }
}
