# Mercurial extension module to be loaded into the "hg serve" process.
# This defines a set of additional commands that the front end uses
from mercurial.hgweb.hgweb_mod import hgweb
from mercurial import templater
import cgi

def head(req):
    req.header([('Content-Type','application/xml')])

# test
def do_kohsuke(self, req):
    head(req)
    req.write("Hello")

# list up all directories. unsorted.
def do_dirtree(self,req):
    ctx = self.changectx(req)
    manifest = ctx.manifest()

    dirs = set()
    for n in manifest.iterkeys():
        # what is the values of the manifest map?
        idx=n.rfind('/')
        # am I handling empty directory at the top level right?
        #  -> hg can't seem to handle empty directories
        if idx>-1:
            dirs.add(n[:idx])
    head(req)

    req.write("<dirs>")
    for k in dirs:
        req.write("<dir name='%s'/>"%escape(k))
    req.write("</dirs>")

def escape(x):
    return cgi.escape(x,True)

hgweb.do_kohsuke = do_kohsuke
hgweb.do_dirtree = do_dirtree