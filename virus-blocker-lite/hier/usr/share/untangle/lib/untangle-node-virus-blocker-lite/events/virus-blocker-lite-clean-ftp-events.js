{
    "category": "Virus Blocker Lite",
    "conditions": [
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","uri","virus_blocker_lite_clean","virus_blocker_lite_name","s_server_addr"],
    "description": "Scanned FTP sessions marked clean.",
    "displayOrder": 32,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "ftp_events",
    "title": "Clean Ftp Events",
    "uniqueId": "virus-blocker-lite-T2Q826GF27"
}