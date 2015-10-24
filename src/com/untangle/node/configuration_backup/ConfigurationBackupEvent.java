/**
 * $Id$
 */
package com.untangle.node.configuration_backup;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * A backup event
 */
@SuppressWarnings("serial")
public class ConfigurationBackupEvent extends LogEvent
{
    private boolean success;
    private String detail;

    public ConfigurationBackupEvent() { }

    public ConfigurationBackupEvent(boolean success, String detail)
    {
        this.success = success;
        this.detail = detail;
    }

    public boolean getSuccess() { return this.success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getDetail() { return this.detail; }
    public void setDetail(String detail) { this.detail = detail; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO reports.configuration_backup_events" + getPartitionTablePostfix() + " " +
            "(time_stamp, success, description) " + 
            "values " +
            "( ?, ?, ? )";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setBoolean(++i, getSuccess());
        pstmt.setString(++i, getDetail());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        if ( getSuccess() )
            action = I18nUtil.marktr("successfully backed up");
        else
            action = I18nUtil.marktr("failed to back up");
            
        String summary = "Configuration Backup " + action;
        return summary;
    }
}
