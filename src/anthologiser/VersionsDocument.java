/*
 * This file is part of Anthologiser.
 * Anthologiser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Anthologiser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Anthologiser.  If not, see <http://www.gnu.org/licenses/>.
 */
package anthologiser;
import calliope.json.JSONDocument;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Represent the versions file in an anthology folder. Versions are just 
 * a set of short names and corresponding long names
 * @author desmond 15/5/2012
 */
public class VersionsDocument extends JSONDocument
{
    File dir;
    static String VERSIONS_FILE = "versions.conf";
    HashMap<String,String> items;
    /**
     * Constructor
     * @param dir
     */
    public VersionsDocument( File dir )
    {
        super();
        items = new HashMap<String,String>();
        this.dir = dir;
    }
    /**
     * Add a version to the set
     * @param shortName the version's short name
     * @param description a long name or description of it
     */
    public void addVersion( String shortName, String description )
    {
        items.put( shortName, description );
        /*Set<String> keys = items.keySet();
        Iterator<String> iter = keys.iterator();
        while ( iter.hasNext() )
        {
            String key = iter.next();
            System.out.println("key="+key+" value="+items.get(key) );
        }*/
    }
    /**
     * Save versions to a file
     * @param dir the directory to save it in
     */
    void externalise() throws Exception
    {
        JSONDocument doc = new JSONDocument();
        // it's always called this
        File dst = new File( dir, VERSIONS_FILE );
        Set<String> keys = items.keySet();
        Iterator<String> iter = keys.iterator();
        ArrayList<JSONDocument> array = new ArrayList<JSONDocument>();
        while ( iter.hasNext() )
        {
            String key = iter.next();
            JSONDocument subDoc = new JSONDocument();
            subDoc.put( JSONKeys.KEY, key );
            subDoc.put( JSONKeys.VALUE, items.get(key) );
            array.add( subDoc );
        }
        doc.put(JSONKeys.VERSIONS, array );
        String text = doc.toString();
        FileOutputStream fos = new FileOutputStream(dst);
        fos.write( text.getBytes("UTF-8") );
        fos.close();
    }
    /**
     * Load an existing versions file
     * @param dir the directory to read from 
     */
    void internalise() throws Exception
    {
        File src = new File( dir, VERSIONS_FILE );
        if ( src.exists() )
        {
            FileInputStream fis = new FileInputStream( src );
            byte[] data = new byte[(int)src.length()];
            fis.read( data );
            String text = new String( data, "UTF-8");
            JSONDocument doc = JSONDocument.internalise( text );
            ArrayList array = (ArrayList)doc.get( JSONKeys.VERSIONS );
            for ( int i=0;i<array.size();i++ )
            {
                JSONDocument subDoc = (JSONDocument)array.get( i );
                items.put( (String)subDoc.get(JSONKeys.KEY), 
                    (String)subDoc.get(JSONKeys.VALUE) );
            }
        }
    }
}
