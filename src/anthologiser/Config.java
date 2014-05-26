/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package anthologiser;

import calliope.json.JSONDocument;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;

/**
 * Represent the config file
 * @author desmond
 */
public class Config 
{
    static String MERGES = "merges";
    static String REMOVALS = "removals";
    static String NAME = "name";
    static String VERSIONS = "versions";
    
    /** regular expression for matching words to remove from poem names */
    String removals;
    /** normalised set of poem names */
    HashMap<String,String> poemNames;
    static Config internalise( File src )
    {
        Config c = new Config();
        JSONDocument jdoc = JSONDocument.internalise( src, "UTF-8" );
        c.poemNames = loadPoemNames( jdoc );
        c.removals = loadRemovals( jdoc );
        return c;
    }
    /**
     * Load the translations of raw to normalised poem names
     * @return a map of actual poem names to their normalised equivalents
     */
    private static HashMap<String,String> loadPoemNames( JSONDocument conf )
    {
        HashMap<String,String> names = new HashMap<String,String>();
        ArrayList list = (ArrayList)conf.get( MERGES );
        for ( int i=0;i<list.size();i++ )
        {
            JSONDocument subDoc = (JSONDocument)list.get(i);
            ArrayList aliases = (ArrayList)subDoc.get(VERSIONS);
            for ( int j=0;j<aliases.size();j++ )
            {
                names.put( (String)aliases.get(j), 
                    (String)subDoc.get(NAME) );
            }
        }
        return names;
    }
    private static String loadRemovals( JSONDocument conf )
    {
        // load removals
        ArrayList matchList = (ArrayList)conf.get( REMOVALS );
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<matchList.size();i++ )
        {
            String item = (String)matchList.get( i );
            if ( sb.length()==0 )
                sb.append( item );
            else
            {
                sb.append( "|" );
                sb.append( item );
            }
        }
        String r = sb.toString();
        r = r.replace("[","\\[");
        r = r.replace("]","\\]");
        return r;
    }
    boolean hasPoemName( String raw )
    {
        return poemNames.containsKey( raw );
    }
    String getPoemKey( String raw )
    {
        return poemNames.get( raw );
    }
    String getRemovals()
    {
        return removals;
    }
}
