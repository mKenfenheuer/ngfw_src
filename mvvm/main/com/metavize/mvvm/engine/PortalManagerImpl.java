/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.ServiceUnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metavize.mvvm.addrbook.AddressBook;
import com.metavize.mvvm.addrbook.AddressBookConfiguration;
import com.metavize.mvvm.addrbook.AddressBookSettings;
import com.metavize.mvvm.addrbook.UserEntry;
import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.portal.Application;
import com.metavize.mvvm.portal.Bookmark;
import com.metavize.mvvm.portal.LocalPortalManager;
import com.metavize.mvvm.portal.PortalGlobal;
import com.metavize.mvvm.portal.PortalGroup;
import com.metavize.mvvm.portal.PortalHomeSettings;
import com.metavize.mvvm.portal.PortalLogin;
import com.metavize.mvvm.portal.PortalLoginEvent;
import com.metavize.mvvm.portal.PortalLogoutEvent;
import com.metavize.mvvm.portal.PortalSettings;
import com.metavize.mvvm.portal.PortalUser;
import com.metavize.mvvm.portal.RemoteApplicationManager;
import com.metavize.mvvm.security.LoginFailureReason;
import com.metavize.mvvm.security.LogoutReason;
import com.metavize.mvvm.util.TransactionWork;
import jcifs.smb.NtlmPasswordAuthentication;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Realm;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.realm.RealmBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Implementation of the PortalManager.
 */
class PortalManagerImpl implements LocalPortalManager
{
    private static final Application[] protoArr = new Application[] { };

    public static final long REAPING_FREQ = 5000;

    // Add two seconds to each failed login attempt to blunt the force
    // of scripted dictionary attacks.
    private static final long LOGIN_FAIL_SLEEP_TIME = 2000;

    private final MvvmContextImpl mvvmContext;
    private final PortalApplicationManagerImpl appManager;
    private final RemotePortalApplicationManagerImpl remoteAppManager;
    private final PortalRealm portalRealm;
    private final AddressBook addressBook;
    private final EventLogger eventLogger;
    private final LoginReaper reaper;

    private final Map<PortalLogin, Long> activeLogins;

    private final Logger logger = Logger.getLogger(PortalManagerImpl.class);

    private PortalSettings portalSettings;

    PortalManagerImpl(MvvmContextImpl mvvmContext)
    {
        this.mvvmContext = mvvmContext;

        activeLogins = new ConcurrentHashMap<PortalLogin, Long>();

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from PortalSettings");
                    portalSettings = (PortalSettings)q.uniqueResult();

                    if (null == portalSettings) {
                        portalSettings = new PortalSettings();
                        PortalGlobal pg = new PortalGlobal();
                        pg.setPortalHomeSettings(new PortalHomeSettings());
                        portalSettings.setGlobal(pg);
                        s.save(portalSettings);
                    }
                    return true;
                }
            };
        mvvmContext.runTransaction(tw);

        appManager = PortalApplicationManagerImpl.applicationManager();
        remoteAppManager = new RemotePortalApplicationManagerImpl(appManager);

        portalRealm = new PortalRealm();

        addressBook = mvvmContext.appAddressBook();

        eventLogger = mvvmContext.eventLogger();

        reaper = new LoginReaper();
        new Thread(reaper).start();

        logger.info("Initialized PortalManager");
    }

    // public methods ---------------------------------------------------------

    public RemoteApplicationManager remoteApplicationManager()
    {
        return remoteAppManager;
    }

    // LocalPortalManager methods ---------------------------------------------

    public PortalApplicationManagerImpl applicationManager()
    {
        return appManager;
    }

    public PortalSettings getPortalSettings()
    {
        return portalSettings;
    }

    public void setPortalSettings(final PortalSettings settings)
    {
        updateIdleTimes(settings);

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    return true;
                }
            };
        mvvmContext.runTransaction(tw);

        this.portalSettings = settings;
    }

    public PortalHomeSettings getPortalHomeSettings(PortalUser user)
    {
        PortalHomeSettings result = user.getPortalHomeSettings();
        if (result == null) {
            PortalGroup userGroup = user.getPortalGroup();
            if (userGroup != null) {
                result = userGroup.getPortalHomeSettings();
            }
        }
        if (result == null) {
            result = portalSettings.getGlobal().getPortalHomeSettings();
        }
        return result;
    }

    public PortalUser getUser(String uid)
    {
        for (PortalUser user : (List<PortalUser>)portalSettings.getUsers()) {
            if (uid.equals(user.getUid())) {
                return user;
            }
        }
        return null;
    }

    public List<Bookmark> getAllBookmarks(PortalUser user)
    {
        List<Bookmark> result = new ArrayList<Bookmark>();

        List userBookmarks = user.getBookmarks();
        if (userBookmarks != null)
            result.addAll(userBookmarks);

        PortalGroup userGroup = user.getPortalGroup();
        if (userGroup != null) {
            List groupBookmarks = userGroup.getBookmarks();
            if (groupBookmarks != null)
                result.addAll(groupBookmarks);
        }

        List globalBookmarks = portalSettings.getGlobal().getBookmarks();
        if (globalBookmarks != null)
            result.addAll(globalBookmarks);

        return result;
    }

    public Bookmark addUserBookmark(final PortalUser user, String name,
                                    Application application, String target)
    {
        Bookmark result = user.addBookmark(name, application, target);

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(user);
                    return true;
                }
            };
        mvvmContext.runTransaction(tw);

        return result;
    }

    public void removeUserBookmark(final PortalUser user, Bookmark bookmark)
    {
        user.removeBookmark(bookmark);

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(user);
                    return true;
                }
            };
        mvvmContext.runTransaction(tw);
    }

    public void logout(PortalLogin login)
    {
        doLogout(login, LogoutReason.USER);
    }

    public void forceLogout(PortalLogin login)
    {
        doLogout(login, LogoutReason.ADMINISTRATOR);
    }

    public List<PortalLogin> getActiveLogins()
    {
        return new ArrayList(activeLogins.keySet());
    }

    // package protected methods ----------------------------------------------

    BasicAuthenticator newPortalAuthenticator()
    {
        return new PortalAuthenticator();
    }

    Realm getPortalRealm()
    {
        return portalRealm;
    }

    void destroy() {
        reaper.destroy();
    }

    // private methods --------------------------------------------------------

    private void updateIdleTimes(PortalSettings settings)
    {
        for (PortalUser pu : (List<PortalUser>)settings.getUsers()) {
            String uid = pu.getUid();
            PortalHomeSettings phs = pu.getPortalHomeSettings();
            if (null != phs) {
                long it = phs.getIdleTimeout();
                for (PortalLogin pl : activeLogins.keySet()) {
                    if (pl.getName().equals(uid)) {
                        Long la = activeLogins.get(pl);
                        if (null != la) {
                            PortalLogin newPl = new PortalLogin(pl, it);
                            activeLogins.put(newPl, la);
                        }
                    }
                }
            }
        }
    }

    private String getCurrentDomain()
    {
        AddressBookSettings s = addressBook.getAddressBookSettings();
        if (AddressBookConfiguration.AD_AND_LOCAL == s.getAddressBookConfiguration()) {
            return s.getADRepositorySettings().getDomain();
        } else {
            return null;
        }
    }

    private long getIdleTimeout(PortalUser user)
    {
        PortalHomeSettings hs = getPortalHomeSettings(user);
        return hs.getIdleTimeout();
    }

    private PortalLogin login(String uid, String password, InetAddress addr)
    {
        try {
            boolean authenticated = addressBook.authenticate(uid, password);

            if (!authenticated) {
                UserEntry ue = addressBook.getEntry(uid);
                if (null == ue) {
                    logger.debug("no user found with login: " + uid);
                    PortalLoginEvent event = new PortalLoginEvent(addr, uid, false, LoginFailureReason.UNKNOWN_USER);
                    eventLogger.log(event);
                } else {
                    logger.debug("password check failed");
                    PortalLoginEvent event = new PortalLoginEvent(addr, uid, false, LoginFailureReason.BAD_PASSWORD);
                    eventLogger.log(event);
                }

                try {
                    Thread.sleep(LOGIN_FAIL_SLEEP_TIME);
                } catch (InterruptedException exn) { }

                return null;
            }
        } catch (ServiceUnavailableException x) {
            logger.error("Unable to authenticate user", x);
            return null;
        }

        PortalUser user = getUser(uid);

        if (null == user && portalSettings.getGlobal().isAutoCreateUsers()) {
            logger.debug(uid + " didn't exist, auto creating");
            PortalUser newUser = portalSettings.addUser(uid);
            setPortalSettings(portalSettings);
            user = newUser;
        }

        if (!user.isLive()) {
            logger.debug("user is disabled");
            PortalLoginEvent event = new PortalLoginEvent(addr, uid, false, LoginFailureReason.DISABLED);
            eventLogger.log(event);
            return null;
        }

        NtlmPasswordAuthentication pwa = null;
        String domain = getCurrentDomain();
        if (null != domain) {
            pwa = new NtlmPasswordAuthentication(domain, uid, password);
        }
        PortalLogin pl = new PortalLogin(user, addr, pwa);
        activeLogins.put(pl, System.currentTimeMillis());

        PortalLoginEvent event = new PortalLoginEvent(addr, uid, true);
        eventLogger.log(event);

        return pl;
    }

    private void doLogout(PortalLogin login, LogoutReason reason)
    {
        if (LogoutReason.ADMINISTRATOR == reason) {
            logger.warn("Administrative logout of " + login);
        } else {
            logger.info("" + reason + " logout of " + login);
        }

        activeLogins.remove(login);

        PortalLogoutEvent evt = new PortalLogoutEvent(login.getClientAddr(),
                                                      login.getUser(), reason);
        eventLogger.log(evt);
    }

    private boolean isSessionLive(PortalLogin pl)
    {
        Long la = activeLogins.get(pl);
        if (null == la) {
            return false;
        } else {
            long ct = System.currentTimeMillis();
            if (ct - la > pl.getIdleTimeout()) {
                activeLogins.put(pl, ct);
                return true;
            } else {
                return false;
            }
        }
    }

    // private classes --------------------------------------------------------

    private class PortalAuthenticator extends BasicAuthenticator
    {
        protected static final String info =
            "com.metavize.mvvm.engine.PortalAuthenticator/4.0";

        private final Log log = LogFactory.getLog(getClass());

        // Realm methods ------------------------------------------------------

        @Override
        public String getInfo()
        {
            return info;
        }

        public boolean authenticate(HttpRequest request, HttpResponse response,
                                    LoginConfig config)
            throws IOException
        {
            HttpServletRequest req = (HttpServletRequest)request.getRequest();
            HttpServletResponse resp = (HttpServletResponse)response.getResponse();
            Principal principal = req.getUserPrincipal();

            String ssoId = (String)request.getNote(Constants.REQ_SSOID_NOTE);

            if (null != principal) {
                if (log.isDebugEnabled()) {
                    log.debug("Already authenticated '" + principal.getName()
                              + "'");
                }

                if (ssoId != null) {
                    associate(ssoId, getSession(request, true));
                }

                if (isSessionLive((PortalLogin)principal)) {
                    return true;
                }
            } else if (null != ssoId) {
                if (log.isDebugEnabled()) {
                    log.debug("SSO Id " + ssoId + " set; attempting " +
                              "reauthentication");
                }

                if (reauthenticateFromSSO(ssoId, request)) {
                    PortalLogin pl = (PortalLogin)req.getUserPrincipal();
                    if (null == pl) {
                        logger.warn("PortalLogin idle time not reset");
                        return true;
                    } else {
                        if (isSessionLive(pl)) {
                            return true;
                        }
                    }
                }
            }

            String authorization = request.getAuthorization();
            String username = parseUsername(authorization);
            String password = parsePassword(authorization);

            LocalPortalManager pm = mvvmContext.portalManager();

            String credentials = password + "," + req.getRemoteAddr();

            principal = context.getRealm().authenticate(username, credentials);

            if (null != principal) {
                register(request, response, principal, Constants.BASIC_METHOD,
                         username, password);
                return true;
            } else {
                String realmName = config.getRealmName();
                if (null == realmName) {
                    realmName = req.getServerName() + ":"
                        + req.getServerPort();
                }

                resp.setHeader("WWW-Authenticate",
                               "Basic realm=\"" + realmName + "\"");
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
        }
    }

    private class PortalRealm extends RealmBase
    {
        // Realm methods ------------------------------------------------------

        public Principal authenticate(String username, String credentials)
        {
            String[] creds = credentials.split(",");
            String password = creds[0];
            InetAddress addr;

            try {
                addr = InetAddress.getByName(creds[1]);
            } catch (UnknownHostException exn) {
                addr = null;
            }

            LocalPortalManager pm = mvvmContext.portalManager();

            return login(username, password, addr);
        }

        @Override
        public boolean hasRole(Principal p, String role)
        {
            return null != role && role.equalsIgnoreCase("user")
                && p instanceof PortalLogin;
        }

        // RealmBase methods --------------------------------------------------

        @Override
        protected String getPassword(String username)
        {
            return null;
        }

        @Override
        protected Principal getPrincipal(String username)
        {
            return null;
        }

        @Override
        protected String getName()
        {
            return "PortalRealm";
        }
    }

    private class LoginReaper implements Runnable
    {
        private volatile Thread thread;

        public void run()
        {
            Thread currentThread = thread = Thread.currentThread();
            while (currentThread == thread) {
                for (PortalLogin pl : activeLogins.keySet()) {
                    if (null != pl) {
                        if (!isSessionLive(pl)) {
                            activeLogins.remove(pl);
                        }
                    }
                }

                try {
                    Thread.sleep(REAPING_FREQ);
                } catch (InterruptedException exn) { /* eval loop cond */ }
            }
        }

        void destroy()
        {
            Thread t = thread;
            thread = null;
            t.interrupt();
        }
    }
}
