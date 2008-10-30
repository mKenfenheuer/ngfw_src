/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.webfilter;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.untangle.node.http.RequestLineToken;
import com.untangle.node.token.Header;
import com.untangle.node.util.CharSequenceUtil;
import com.untangle.uvm.node.IPMaddrRule;
import com.untangle.uvm.node.MimeType;
import com.untangle.uvm.node.MimeTypeRule;
import com.untangle.uvm.node.StringRule;
import org.apache.log4j.Logger;

/**
 * Does blacklist lookups in the database.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class Blacklist
{
    private static final Pattern IP_PATTERN = Pattern
        .compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    private final Logger logger = Logger.getLogger(Blacklist.class);

    private final WebFilterBase node;

    private final Map<InetAddress, Set<String>> hostWhitelists
        = new HashMap<InetAddress, Set<String>>();

    private volatile WebFilterSettings settings;
    private volatile String[] blockedUrls = new String[0];
    private volatile String[] passedUrls = new String[0];

    // constructors -----------------------------------------------------------

    public Blacklist(WebFilterBase node)
    {
        this.node = node;
    }

    // abstract methods -------------------------------------------------------

    public abstract void open();
    public abstract void close();

    // blacklist methods ------------------------------------------------------

    public void configure(WebFilterSettings settings)
    {
        this.settings = settings;
    }

    public synchronized void reconfigure()
    {
        blockedUrls = makeCustomList(settings.getBlockedUrls());
        passedUrls = makeCustomList(settings.getPassedUrls());

        doReconfigure();
    }

    // protected methods ------------------------------------------------------

    protected abstract void doReconfigure();

    // package protected methods ----------------------------------------------

    void addWhitelistHost(InetAddress addr, String site)
    {
        Set<String> wl;
        synchronized (hostWhitelists) {
            wl = hostWhitelists.get(addr);
            if (null == wl) {
                wl = new HashSet<String>();
                hostWhitelists.put(addr, wl);
            }
        }

        synchronized (wl) {
            wl.add(site);
        }
    }

    /**
     * Checks if the request should be blocked, giving an appropriate
     * response if it should.
     *
     * @param host the requested host.
     * @param path the requested path.
     * @return an HTML response.
     */
    public String checkRequest(InetAddress clientIp, int port,
                               RequestLineToken requestLine, Header header)
    {
        URI uri = requestLine.getRequestUri().normalize();

        String path = uri.getPath();
        path = null == path ? "" : uri.getPath().toLowerCase();

        String host = uri.getHost();
        if (null == host) {
            host = header.getValue("host");
            if (null == host) {
                host = clientIp.getHostAddress();
            }
        }

        host = host.toLowerCase();
        while (0 < host.length() && '.' == host.charAt(host.length() - 1)) {
            host = host.substring(0, host.length() - 1);
        }

        String passCategory = passClient(clientIp);

        if (null != passCategory) {
            WebFilterEvent hbe = new WebFilterEvent
                (requestLine.getRequestLine(), Action.PASS, Reason.PASS_CLIENT,
                 passCategory);
            logger.info(hbe);
            return null;
        } else {
            String dom = host;

            if (isUserWhitelistedDomain(dom, clientIp)) {
                WebFilterEvent hbe = new WebFilterEvent
                    (requestLine.getRequestLine(), Action.PASS,
                     Reason.PASS_URL, "unblocked temporarily");
                node.log(hbe);

                return null;
            } else {
                while (null != dom) {
                    StringRule sr = findCategory(passedUrls, dom + path,
                                                 settings.getPassedUrls());
                    String category = null == sr ? null : sr.getDescription();

                    if (null != category) {
                        WebFilterEvent hbe = new WebFilterEvent
                            (requestLine.getRequestLine(), Action.PASS,
                             Reason.PASS_URL, category);
                        node.log(hbe);

                        return null;
                    }
                    dom = nextHost(dom);
                }
            }
        }

        if (settings.getBaseSettings().getBlockAllIpHosts()) {
            if (null == host || IP_PATTERN.matcher(host).matches()) {
                WebFilterEvent hbe = new WebFilterEvent
                    (requestLine.getRequestLine(), Action.BLOCK,
                     Reason.BLOCK_IP_HOST, host);
                node.log(hbe);

                WebFilterBlockDetails bd = new WebFilterBlockDetails
                    (settings, host, uri.toString(),
                     "host name is an IP address (" + host + ")", clientIp);
                return node.generateNonce(bd);
            }
        }

        // check in WebFilterSettings
        String nonce = checkBlacklist(clientIp, host, port, requestLine);

        if (null != nonce) {
            return nonce;
        }

        // Check Extensions
        for (StringRule rule : settings.getBlockedExtensions()) {
            String exn = rule.getString().toLowerCase();
            if (rule.isLive() && path.endsWith(exn)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("blocking extension " + exn);
                }
                WebFilterEvent hbe = new WebFilterEvent
                    (requestLine.getRequestLine(), Action.BLOCK,
                     Reason.BLOCK_EXTENSION, exn);
                node.log(hbe);

                WebFilterBlockDetails bd = new WebFilterBlockDetails
                    (settings, host, uri.toString(),
                     "extension (" + exn + ")", clientIp);
                return node.generateNonce(bd);
            }
        }

        return null;
    }

    public String checkResponse(InetAddress clientIp,
                                RequestLineToken requestLine, Header header)
    {
        if (null == requestLine) {
            return null;
        } else if (null != passClient(clientIp)) {
            return null;
        }

        String contentType = header.getValue("content-type");

        for (MimeTypeRule rule : settings.getBlockedMimeTypes()) {
            MimeType mt = rule.getMimeType();
            if (rule.isLive() && mt.matches(contentType)) {
                WebFilterEvent hbe = new WebFilterEvent
                    (requestLine.getRequestLine(), Action.BLOCK,
                     Reason.BLOCK_MIME, contentType);
                node.log(hbe);
                String host = header.getValue("host");
                URI uri = requestLine.getRequestUri().normalize();

                WebFilterBlockDetails bd = new WebFilterBlockDetails
                    (settings, host, uri.toString(),
                     "Mime-Type (" + contentType + ")", clientIp);
                return node.generateNonce(bd);
            }
        }

        WebFilterEvent e = new WebFilterEvent(requestLine.getRequestLine(),
                                              null, null, null, true);
        node.log(e);

        return null;
    }

    // protected methods ------------------------------------------------------

    protected abstract String checkBlacklistDatabase(String dom, int port,
                                                     String uri);

    protected abstract void updateToCurrentCategories(WebFilterSettings s);

    protected WebFilterSettings getSettings()
    {
        return settings;
    }

    protected String mostSpecificCategory(List<String> catNames)
    {
        if (catNames != null) {
            for (String catName : catNames) {
                BlacklistCategory bc = getSettings().getBlacklistCategory(catName);

                if (null != bc && (bc.getBlock() || bc.getLog())) {
                    return catName;
                }
             }
        }

        return BlacklistCategory.UNCATEGORIZED;
    }

    // private methods --------------------------------------------------------

    /**
     * Check if client is whitelisted.
     *
     * @param clientIp address of the client machine.
     * @return true if the client is whitelisted.
     */
    private String passClient(InetAddress clientIp)
    {
        for (IPMaddrRule rule : settings.getPassedClients()) {
            if (rule.getIpMaddr().contains(clientIp) && rule.isLive()) {
                return rule.getDescription();
            }
        }

        return null;
    }

    private String checkBlacklist(InetAddress clientIp, String host, int port,
                                  RequestLineToken requestLine)
    {
        String uri = requestLine.getRequestUri().normalize().toString();
        String category = null;
        StringRule stringRule = null;
        Reason reason = null;

        String dom = host;
        while (null == category && null != dom) {
            String url = dom + uri;

            stringRule = findCategory(blockedUrls, url,
                                      settings.getBlockedUrls());
            category = null == stringRule ? null : stringRule.getDescription();
            if (null != category) {
                reason = Reason.BLOCK_URL;
            } else {
                category = checkBlacklistDatabase(dom, port, uri);

                if (null != category) {
                    reason = Reason.BLOCK_CATEGORY;
                }
            }

            if (null == category) {
                dom = nextHost(dom);
            }
        }

        if (null != category) {
            BlacklistCategory bc = settings.getBlacklistCategory(category);

            if (null != bc) {
                Action a = bc.getBlock() ? Action.BLOCK : Action.PASS;
                WebFilterEvent hbe = new WebFilterEvent
                    (requestLine.getRequestLine(), a, reason, bc.getDisplayName());
                node.log(hbe);
            } else if (null == stringRule || stringRule.getLog()) {
                WebFilterEvent hbe = new WebFilterEvent
                    (requestLine.getRequestLine(), Action.BLOCK, reason, category);
                node.log(hbe);
            }

            if ((null != bc && bc.getBlock())
                || (null != stringRule && stringRule.isLive())) {
                WebFilterBlockDetails bd = new WebFilterBlockDetails
                    (settings, host, uri, null == bc ? category : bc.getDisplayName(), clientIp);
                return node.generateNonce(bd);
            } else {
                return null;
            }
        }

        return null;
    }

    private StringRule findCategory(CharSequence[] strs, String val,
                                    Set<StringRule> rules)
    {
        int i = findMatch(strs, val);
        return 0 > i ? null : lookupCategory(strs[i], rules);
    }

    private String[] findCategories(Map<String, CharSequence[]> cats, String val)
    {
        List<String> result = new ArrayList<String>();
        for (String cat : cats.keySet()) {
            CharSequence[] strs = cats.get(cat);
            int i = findMatch(strs, val);
            if (0 <= i) {
                result.add(cat);
            }
        }
        if (result.size() == 0) {
            return null;
        } else {
            return (String[]) result.toArray(new String[result.size()]);
        }
    }

    private int findMatch(CharSequence[] strs, String val)
    {
        if (null == val || null == strs) {
            return -1;
        }

        int i = Arrays.binarySearch(strs, val, CharSequenceUtil.COMPARATOR);
        if (0 <= i) {
            return i;
        } else {
            int j = -i - 2;
            if (0 <= j && j < strs.length
                && CharSequenceUtil.startsWith(val, strs[j])) {
                return j;
            }
        }

        return -1;
    }

    private StringRule lookupCategory(CharSequence match,
                                      Set<StringRule> rules)

    {
        for (StringRule rule : rules) {
            String uri = normalizeDomain(rule.getString());

            if ((rule.isLive() || rule.getLog()) && match.equals(uri)) {
                return rule;
            }
        }

        return null;
    }

    /**
     * Gets the next domain stripping off the lowest level domain from
     * host. Does not return the top level domain. Returns null when
     * no more domains are left.
     *
     * <b>This method assumes trailing dots are stripped from host.</b>
     *
     * @param host a <code>String</code> value
     * @return a <code>String</code> value
     */
    // XXX duplicated code
    private String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (-1 == i) {
            return null;
        } else {
            int j = host.indexOf('.', i + 1);
            if (-1 == j) { // skip tld
                return null;
            }

            return host.substring(i + 1);
        }
    }

    private String[] makeCustomList(Set<StringRule> rules)
    {
        List<String> strings = new ArrayList<String>(rules.size());
        for (StringRule rule : rules) {
            if (rule.isLive()) {
                String uri = normalizeDomain(rule.getString());
                strings.add(uri);
            }
        }
        Collections.sort(strings);

        return strings.toArray(new String[strings.size()]);
    }

    private String normalizeDomain(String dom)
    {
        String url = dom.toLowerCase();
        String uri = url.startsWith("http://")
            ? url.substring("http://".length()) : url;

        while (0 < uri.length()
               && ('*' == uri.charAt(0) || '.' == uri.charAt(0))) {
            uri = uri.substring(1);
        }

        if (uri.startsWith("www.")) {
            uri = uri.substring("www.".length());
        }

        return uri;
    }

    private boolean isUserWhitelistedDomain(String domain, InetAddress clientAddr)
    {
        if (null == domain) {
            return false;
        } else {
            domain = domain.toLowerCase();

            Set<String> l = hostWhitelists.get(clientAddr);
            if (null == l) {
                return false;
            } else {
                return findMatch(l, domain);
            }
        }
    }

    private boolean findMatch(Set<String> rules, String domain)
    {
        for (String d = domain; null != d; d = nextHost(d)) {
            if (rules.contains(d)) {
                return true;
            }
        }

        return false;
    }
}
