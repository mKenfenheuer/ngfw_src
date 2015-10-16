import unittest2
import time
import subprocess
from datetime import datetime
import sys
import os
import subprocess
import socket

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import remote_control
import global_functions

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
node = None
testsite = "test.untangle.com"
testsiteIP = socket.gethostbyname(testsite)

def addPassSite(site, enabled=True, description="description"):
    newRule =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.node.GenericRule", "string": site }
    rules = node.getPassSites()
    rules["list"].append(newRule)
    node.setPassSites(rules)

def nukePassSites():
    rules = node.getPassSites()
    rules["list"] = []
    node.setPassSites(rules)

class VirusTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-base-virus"

    @staticmethod
    def shortName():
        return "untangle"

    @staticmethod
    def displayName():
        return "Virus Blocker Lite"

    def setUp(self):
        global node,md5StdNum
        if node == None:
            # download eicar and trojan files before installing virus blocker
            remote_control.runCommand("rm -f /tmp/eicar /tmp/std_022_ftpVirusBlocked_file /tmp/temp_022_ftpVirusPassSite_file")
            result = remote_control.runCommand("wget -q -O /tmp/eicar http://test.untangle.com/virus/eicar.com")
            assert (result == 0)
            result = remote_control.runCommand("wget -q -O /tmp/std_022_ftpVirusBlocked_file ftp://test.untangle.com/virus/fedexvirus.zip")
            assert (result == 0)
            md5StdNum = remote_control.runCommand("\"md5sum /tmp/std_022_ftpVirusBlocked_file | awk '{print $1}'\"", stdout=True)
            print "md5StdNum <%s>" % md5StdNum
            assert (result == 0)
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName();
                raise unittest2.SkipTest('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)

    # test that client can http download zip
    def test_011_httpNonVirusNotBlocked(self):
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.zip 2>&1 | grep -q text123")
        assert (result == 0)

    # test that client can http download pdf
    def test_013_httpNonVirusPDFNotBlocked(self):
        result = remote_control.runCommand("wget -q -O /dev/null http://test.untangle.com/test/test.pdf")
        assert (result == 0)

    # test that client can block virus http download zip
    def test_015_httpEicarBlocked(self):
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/virus/eicar.zip 2>&1 | grep -q blocked")
        assert (result == 0)

    # test that client can block virus http download zip
    def test_016_httpVirusBlocked(self):
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/virus/virus.exe 2>&1 | grep -q blocked")
        assert (result == 0)

    # test that client can block virus http download zip
    def test_017_httpVirusZipBlocked(self):
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/virus/fedexvirus.zip 2>&1 | grep -q blocked")
        assert (result == 0)

    # test that client can block a partial fetch after full fetch (using cache)
    def test_018_httpPartialVirusBlockedWithCache(self):
        result = remote_control.runCommand("curl -L http://test.untangle.com/virus/virus.exe 2>&1 | grep -q blocked")
        assert (result == 0)
        result = remote_control.runCommand("curl -L -r '5-' http://test.untangle.com/virus/virus.exe 2>&1 | grep -q blocked")
        assert (result == 0)

    # test that client can block virus http download zip
    def test_019_httpEicarPassSite(self):
        addPassSite("test.untangle.com")
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/virus/eicar.zip 2>&1 | grep -q blocked")
        nukePassSites()
        assert (result == 1)

    # test that client can ftp download zip
    def test_021_ftpNonVirusNotBlocked(self):
        adResult = subprocess.call(["ping","-c","1","test.untangle.com"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (adResult != 0):
            raise unittest2.SkipTest("FTP server not available")
        result = remote_control.runCommand("wget -q -O /dev/null ftp://test.untangle.com/test.zip")
        assert (result == 0)

    # test that client can ftp download PDF
    def test_023_ftpNonVirusPDFNotBlocked(self):
        adResult = subprocess.call(["ping","-c","1","test.untangle.com"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (adResult != 0):
            raise unittest2.SkipTest("FTP server not available")
        result = remote_control.runCommand("wget -q -O /dev/null ftp://test.untangle.com/test/test.pdf")
        assert (result == 0)

    # test that client can block virus ftp download zip
    def test_025_ftpVirusBlocked(self):
        remote_control.runCommand("rm -f /tmp/temp_022_ftpVirusBlocked_file") 
        result = remote_control.runCommand("wget -q -O /tmp/temp_022_ftpVirusBlocked_file ftp://test.untangle.com/virus/fedexvirus.zip")
        assert (result == 0)
        md5TestNum = remote_control.runCommand("\"md5sum /tmp/temp_022_ftpVirusBlocked_file | awk '{print $1}'\"", stdout=True)
        print "md5StdNum <%s> vs md5TestNum <%s>" % (md5StdNum, md5TestNum)
        assert (md5StdNum != md5TestNum)

        events = global_functions.get_events(self.displayName(),'Infected Ftp Events',None,1)
        assert(events != None)
        ftp_server_IP = socket.gethostbyname("test.untangle.com")
        found = global_functions.check_events( events.get('list'), 5, 
                                            "s_server_addr", ftp_server_IP, 
                                            "c_client_addr", remote_control.clientIP, 
                                            "uri", "fedexvirus.zip",
                                            self.shortName() + '_clean', False )
        assert( found )

    # test that client can block virus ftp download zip
    def test_027_ftpVirusPassSite(self):
        ftp_server_IP = socket.gethostbyname("test.untangle.com")
        addPassSite(ftp_server_IP)
        remote_control.runCommand("rm -f /tmp/temp_022_ftpVirusBlocked_file") 
        result = remote_control.runCommand("wget -q -O /tmp/temp_022_ftpVirusPassSite_file ftp://test.untangle.com/virus/fedexvirus.zip")
        assert (result == 0)
        md5TestNum = remote_control.runCommand("\"md5sum /tmp/temp_022_ftpVirusPassSite_file | awk '{print $1}'\"", stdout=True)
        print "md5StdNum <%s> vs md5TestNum <%s>" % (md5StdNum, md5TestNum)
        assert (md5StdNum == md5TestNum)

        events = global_functions.get_events(self.displayName(),'Infected Ftp Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "s_server_addr", ftp_server_IP, 
                                            "c_client_addr", remote_control.clientIP, 
                                            "uri", "fedexvirus.zip",
                                            self.shortName() + '_clean', False )
        assert( found )
        nukePassSites()

    def test_100_eventlog_httpVirus(self):
        fname = sys._getframe().f_code.co_name
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/virus/eicar.zip?arg=%s 2>&1 | grep -q blocked" % fname)
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Infected Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "host", "test.untangle.com", 
                                            "uri", ("/virus/eicar.zip?arg=%s" % fname),
                                            self.shortName() + '_clean', False )
        assert( found )

    def test_101_eventlog_httpNonVirus(self):
        fname = sys._getframe().f_code.co_name
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.zip?arg=%s 2>&1 | grep -q text123" % fname)
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Clean Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "host", "test.untangle.com", 
                                            "uri", ("/test/test.zip?arg=%s" % fname),
                                            self.shortName() + '_clean', True )
        assert( found )

    def test_102_eventlog_ftpVirus(self):
        fname = sys._getframe().f_code.co_name
        result = remote_control.runCommand("wget -q -O /tmp/temp_022_ftpVirusBlocked_file ftp://test.untangle.com/virus/fedexvirus.zip")
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Infected Ftp Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "uri", "fedexvirus.zip",
                                            self.shortName() + '_clean', False )
        assert( found )

    def test_103_eventlog_ftpNonVirus(self):
        fname = sys._getframe().f_code.co_name
        result = remote_control.runCommand("wget -q -O /dev/null ftp://test.untangle.com/test.zip")
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Clean Ftp Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "uri", "test.zip",
                                            self.shortName() + '_clean', True )
        assert( found )

    port25Test = subprocess.call(["netcat","-z","-w","1","test.untangle.com","25"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    @unittest2.skipIf(port25Test != 0,  "Port 25 blocked")
    def test_104_eventlog_smtpVirus(self):
        startTime = datetime.now()
        fname = sys._getframe().f_code.co_name
        # download the email script
        result = remote_control.runCommand("wget -q -O /tmp/email_script.py http://test.untangle.com/test/email_script.py")
        assert (result == 0)
        result = remote_control.runCommand("chmod 775 /tmp/email_script.py")
        assert (result == 0)
        # email the file
        result = remote_control.runCommand("/tmp/email_script.py --server=%s --from=junk@test.untangle.com --to=junk@test.untangle.com --subject='%s' --body='body' --file=/tmp/eicar" % (testsiteIP, fname))
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Infected Email Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "addr", "junk@test.untangle.com", 
                                            "subject", str(fname), 
                                            self.shortName() + '_clean', False,
                                            min_date=startTime )
        assert( found )

    port25Test = subprocess.call(["netcat","-z","-w","1","test.untangle.com","25"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    @unittest2.skipIf(port25Test != 0,  "Port 25 blocked")
    def test_105_eventlog_smtpNonVirus(self):
        startTime = datetime.now()
        fname = sys._getframe().f_code.co_name
        result = remote_control.runCommand("echo '%s' > /tmp/attachment-%s" % (fname, fname))
        assert (result == 0)
        # download the email script
        result = remote_control.runCommand("wget -q -O /tmp/email_script.py http://test.untangle.com/test/email_script.py")
        assert (result == 0)
        result = remote_control.runCommand("chmod 775 /tmp/email_script.py")
        assert (result == 0)
        # email the file
        result = remote_control.runCommand("/tmp/email_script.py --server=%s --from=junk@test.untangle.com --to=junk@test.untangle.com --subject='%s' --body='body' --file=/tmp/attachment-%s" % (testsiteIP, fname, fname))
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Clean Email Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "addr", "junk@test.untangle.com", 
                                            "subject", str(fname), 
                                            self.shortName() + '_clean', True,
                                            min_date=startTime )
        assert( found )

    port25Test = subprocess.call(["netcat","-z","-w","1","test.untangle.com","25"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    @unittest2.skipIf(port25Test != 0,  "Port 25 blocked")
    def test_106_eventlog_smtpVirusPassList(self):
        ftp_server_IP = socket.gethostbyname("test.untangle.com")
        addPassSite(ftp_server_IP)
        startTime = datetime.now()
        fname = sys._getframe().f_code.co_name
        result = remote_control.runCommand("echo '%s' > /tmp/attachment-%s" % (fname, fname))
        assert (result == 0)
        # download the email script
        result = remote_control.runCommand("wget -q -O /tmp/email_script.py http://test.untangle.com/test/email_script.py")
        assert (result == 0)
        result = remote_control.runCommand("chmod 775 /tmp/email_script.py")
        assert (result == 0)
        # email the file
        result = remote_control.runCommand("/tmp/email_script.py --server=%s --from=junk@test.untangle.com --to=junk@test.untangle.com --subject='%s' --body='body' --file=/tmp/eicar" % (testsiteIP, fname))
        assert (result == 0)

        events = global_functions.get_events(self.displayName(),'Clean Email Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "addr", "junk@test.untangle.com", 
                                            "subject", str(fname), 
                                            self.shortName() + '_clean', True,
                                            min_date=startTime )
        assert( found )
        nukePassSites()

    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None
        