package service;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.service.AbstractUserService;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.AuthUserIdentity;
import com.feth.play.module.pa.user.EmailIdentity;
import models.LinkedAccount;
import models.Organization;
import models.User;
import play.Logger;
import play.mvc.Http;

import javax.inject.Inject;

public class MyUserService extends AbstractUserService {

	public final static String DUMMY_USERNAME = "__DUMMY_USERNAME__";
	static Logger.ALogger sLogger = Logger.of("application");

    @Inject
    public MyUserService(final PlayAuthenticate auth) {
        super(auth);
    }

    // We do not create new user accounts. If you aren't already approved,
    // you can't log in.
	@Override
	public Object save(final AuthUser authUser) {
        sLogger.debug("MyUserService::save " + authUser);
		final boolean isLinked = User.existsByAuthUserIdentity(authUser);
		if (!isLinked) {
            if (authUser instanceof EmailIdentity) {
                final EmailIdentity identity = (EmailIdentity) authUser;
                sLogger.debug("    is email identity, email='" + identity.getEmail() + "'");
                User u = User.find.query().where().ieq("email", identity.getEmail()).findOne();
                if (u != null) {
                    sLogger.debug("    found user by email");
                } else {
                	sLogger.debug("    creating new account");
                	Organization org = Organization.getByHost(Http.Context.current().request());
                	sLogger.error("New login from unknown user: " + identity.getEmail() + ", org: " + org.name);
                	u = User.create(identity.getEmail(), DUMMY_USERNAME, org);
                }
                u.linkedAccounts.add(LinkedAccount.create(authUser));
                u.save();
                return u;
            }
		}

        return null;
	}

	@Override
	public Object getLocalIdentity(final AuthUserIdentity identity) {
        sLogger.debug("MyUserService::getLocalIdentity " + identity);
		// For production: Caching might be a good idea here...
		// ...and dont forget to sync the cache when users get deactivated/deleted
		final User u = User.findByAuthUserIdentity(identity);
		if(u != null) {
            sLogger.debug("    found user by auth identity");
			return u.id;
		} else {
			return null;
		}
	}

	@Override
	public AuthUser merge(final AuthUser newUser, final AuthUser oldUser) {
		if (!oldUser.equals(newUser)) {
			User.merge(oldUser, newUser);
		}
		return oldUser;
	}

	@Override
	public AuthUser link(final AuthUser oldUser, final AuthUser newUser) {
		User.addLinkedAccount(oldUser, newUser);
		return null;
	}

}
