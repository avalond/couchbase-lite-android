package com.couchbase.lite.testapp.tests;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Status;
import com.couchbase.lite.internal.Body;
import com.couchbase.lite.internal.RevisionInternal;
import com.couchbase.lite.util.Log;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;


public class LocalDocs extends CBLiteTestCase {

    public static final String TAG = "LocalDocs";

    public void testLocalDocs() throws CouchbaseLiteException {

        //create a document
        Map<String, Object> documentProperties = new HashMap<String, Object>();
        documentProperties.put("_id", "_local/doc1");
        documentProperties.put("foo", 1);
        documentProperties.put("bar", false);

        Body body = new Body(documentProperties);
        RevisionInternal rev1 = new RevisionInternal(body, database);

        Status status = new Status();
        rev1 = database.putLocalRevision(rev1, null);

        Log.v(TAG, "Created " + rev1);
        Assert.assertEquals("_local/doc1", rev1.getDocId());
        Assert.assertTrue(rev1.getRevId().startsWith("1-"));

        //read it back
        RevisionInternal readRev = database.getLocalDocument(rev1.getDocId(), null);
        Assert.assertNotNull(readRev);
        Map<String,Object> readRevProps = readRev.getProperties();
        Assert.assertEquals(rev1.getDocId(), readRev.getProperties().get("_id"));
        Assert.assertEquals(rev1.getRevId(), readRev.getProperties().get("_rev"));
        Assert.assertEquals(userProperties(readRevProps), userProperties(body.getProperties()));

        //now update it
        documentProperties = readRev.getProperties();
        documentProperties.put("status", "updated!");
        body = new Body(documentProperties);
        RevisionInternal rev2 = new RevisionInternal(body, database);
        RevisionInternal rev2input = rev2;
        rev2 = database.putLocalRevision(rev2, rev1.getRevId());
        Log.v(TAG, "Updated " + rev1);
        Assert.assertEquals(rev1.getDocId(), rev2.getDocId());
        Assert.assertTrue(rev2.getRevId().startsWith("2-"));

        //read it back
        readRev = database.getLocalDocument(rev2.getDocId(), null);
        Assert.assertNotNull(readRev);
        Assert.assertEquals(userProperties(readRev.getProperties()), userProperties(body.getProperties()));

        // Try to update the first rev, which should fail:
        boolean gotException = false;
        try {
            database.putLocalRevision(rev2input, rev1.getRevId());
        } catch (CouchbaseLiteException e) {
            Assert.assertEquals(Status.CONFLICT, e.getCBLStatus().getCode());
            gotException = true;
        }
        Assert.assertTrue(gotException);


        // Delete it:
        RevisionInternal revD = new RevisionInternal(rev2.getDocId(), null, true, database);

        gotException = false;
        try {
            RevisionInternal revResult = database.putLocalRevision(revD, null);
            Assert.assertNull(revResult);
        } catch (CouchbaseLiteException e) {
            Assert.assertEquals(Status.CONFLICT, e.getCBLStatus().getCode());
            gotException = true;
        }
        Assert.assertTrue(gotException);

        revD = database.putLocalRevision(revD, rev2.getRevId());

        // Delete nonexistent doc:
        gotException = false;
        RevisionInternal revFake = new RevisionInternal("_local/fake", null, true, database);
        try {
            database.putLocalRevision(revFake, null);
        } catch (CouchbaseLiteException e) {
            Assert.assertEquals(Status.NOT_FOUND, e.getCBLStatus().getCode());
            gotException = true;
        }
        Assert.assertTrue(gotException);


        // Read it back (should fail):
        readRev = database.getLocalDocument(revD.getDocId(), null);
        Assert.assertNull(readRev);
    }

}