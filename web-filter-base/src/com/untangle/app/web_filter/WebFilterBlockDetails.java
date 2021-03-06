/**
 * $Id$
 */

package com.untangle.app.web_filter;

import java.net.InetAddress;

import com.untangle.app.http.BlockDetails;

/**
 * BlockDetails for WebFilter.
 */
@SuppressWarnings("serial")
public class WebFilterBlockDetails extends BlockDetails
{
    private final WebFilterSettings settings;
    private final String reason;
    private final InetAddress clientAddr;
    private final String appTitle;

    /**
     * Constructor
     * 
     * @param settings
     *        The weeb filter settings
     * @param host
     *        The host
     * @param uri
     *        The URI
     * @param reason
     *        The reason
     * @param clientAddr
     *        The client address
     * @param appTitle
     *        The application title
     */
    public WebFilterBlockDetails(WebFilterSettings settings, String host, String uri, String reason, InetAddress clientAddr, String appTitle)
    {
        super(host, uri);
        this.settings = settings;
        this.reason = reason;
        this.clientAddr = clientAddr;
        this.appTitle = appTitle;
    }

    /**
     * Get the header
     * 
     * @return The header
     */
    public String getHeader()
    {
        return "Web Filter";
    }

    /**
     * Get the reason
     * 
     * @return The reason
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Get the app title
     * 
     * @return The title
     */
    public String getAppTitle()
    {
        return appTitle;
    }

    /**
     * Get the client address
     * 
     * @return The client address
     */
    public InetAddress getClientAddress()
    {
        return clientAddr;
    }

    /**
     * Get the settings
     * 
     * @return The settings
     */
    public WebFilterSettings getSettings()
    {
        return settings;
    }
}
