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
import calliope.json.corcode.STILDocument;
import calliope.json.corcode.Range;
import calliope.json.corcode.Annotation;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
/**
 * Represent an anthology file consisting of a plain text file and a 
 * separate STIL markup set in JSON. Preserve the order of poems added to 
 * the anthology.
 * structure is:
 * anthologies
 *     |
 *     parent ----- [MVD]
 *                    |
 *                    -------cortex.mvd
 *                    |
 *                    -------corcode
 *                              |
 *                              ------- corcode1.json
 * @author desmond 15/5/2012
 */
public class Anthology 
{
    static String CORTEX_MVD = "cortex.mvd";
    static String CORCODE = "corcode";
    String linkBase;
    /** anthology title */
    String title;
    /** anthology description - usually MS library entry */
    String description;
    File src;
    /** markup source file */
    File msrc;
    /** anthology dir */
    File parent,corTexFile,corCodeDir;
    File mvdDir;
    /** order-preserving array */
    ArrayList<String> order;
    /** the name of this anthology */
    String name;
    /** relative links to resources in database, ensures uniqueness */
    HashMap<String,String> links;
    /**
     * Read in an existing anthology or create an empty one
     * @param dir the directory where the anthology files reside
     * @param simpleName short name of the anthology
     * @param linnkBase the prefix for all links
     */
    public Anthology( File dir, String simpleName, String linkBase ) throws Exception
    {
        links = new HashMap<String,String>();
        order = new ArrayList<String>();
        this.linkBase = linkBase;
        this.parent = new File( dir, "%"+simpleName );
        this.name = simpleName;
        title = "";
        mvdDir = new File(parent, "MVD" );
        corTexFile = new File( mvdDir, CORTEX_MVD );
        corCodeDir = new File( mvdDir, CORCODE );
        src = new File(mvdDir,CORTEX_MVD );
        msrc = new File(corCodeDir,name+"-markup.json" );
        internalise();
    }
    /**
     * Set this anthology's title
     * @param title the name for the collection as stated in the source
     */
    public void setTitle( String title )
    {
        this.title = title;
    }
    /**
     * Has the description already been set?
     * @return true if it has
     */
    public boolean descriptionSet()
    {
        return description != null && description.length()>0;
    }
    /**
     * Has the title already been set?
     * @return true if it has
     */
    public boolean titleSet()
    {
        return title != null && title.length()>0;
    }
    /**
     * Get the description string
     * @param content description of the anthology as stated in the source
     */
    public void setDescription( String content )
    {
        this.description = content;
    }
    /**
     * Get the prefix for all links
     * @return a String
     */
    String getLinkBase()
    {
        return linkBase;
    }
    /**
     * Add a work name to the anthology
     * @param name name of a work
     * @param link link to the resource
     */
    public void addItem( String name, String link )
    {
        // avoid duplicates, like hashmap
        if ( !links.containsKey(name) )
            order.add( name );
        links.put( name, link );
    }
    /**
     * Write out this anthology
     */
    void externalise() throws Exception
    {
        boolean res = true;
        if ( !parent.exists() )
            res = parent.mkdir();
        if ( !res )
            throw new Exception("failed to create directory "+parent.getName());
        if ( !mvdDir.exists() )
            res = mvdDir.mkdir();
        if ( !res )
            throw new Exception("failed to create directory "+mvdDir.getName());
        if ( !corCodeDir.exists() )
            res = corCodeDir.mkdir();
        if ( !res )
            throw new Exception("failed to create directory "+corCodeDir.getName());
        // construct text and markup
        int pos = 0;
        StringBuilder sb = new StringBuilder();
        STILDocument markup = new STILDocument();
        sb.append(title);
        sb.append("\n");
        markup.add( new Range(JSONKeys.TITLE, 0, title.length()+1) );
        pos += title.length()+1;
        sb.append( description );
        sb.append( "\n" );
        if ( description == null )
            description = "";
        markup.add( new Range(JSONKeys.DESCRIPTION, pos, description.length()+1) );
        pos += description.length()+1;
        Set<String> keys = links.keySet();
        for ( int i=0;i<order.size();i++ )
        {
            String key = order.get( i );
            sb.append( key );
            sb.append( "\n" );
            Range r = new Range(JSONKeys.ENTRY, pos, key.length()+1 );
            String value = links.get(key).replace(" ","%20");
            r.addAnnotation( JSONKeys.LINK, value );
            pos += key.length()+1;
            markup.add( r );
        }
        // write out the text
        JSONDocument doc = new JSONDocument();
        doc.add( JSONKeys.FORMAT, "text/plain", false );
        doc.add( JSONKeys.TITLE, title, false );
        doc.add( JSONKeys.BODY, sb.toString(), false );
        if ( !src.exists() )
        {
            res = true;
            if ( !src.getParentFile().exists() )
            {
                res = src.getParentFile().mkdirs();
                if ( !res )
                    throw new Exception("failed to create "+src.getParent());
            }
            res = src.createNewFile();
            if ( !res )
                throw new Exception("failed to create "+src.getName());
        }
        FileOutputStream fos = new FileOutputStream( src );
        String docContent = doc.toString();
        fos.write( docContent.getBytes("UTF-8") );
        fos.close();
        // write out the markup file for external programs
        if ( !msrc.exists() )
        {
            res = true;
            if ( !msrc.getParentFile().exists() )
            {
                res = msrc.getParentFile().mkdirs();
                if ( !res )
                    throw new Exception("failed to create "+msrc.getParent());
            }
            res = msrc.createNewFile();
            if ( !res )
                throw new Exception("failed to create "+msrc.getName());
        }
        fos = new FileOutputStream( msrc );
        String markupString = markup.toString();
        fos.write( markupString.getBytes("UTF-8") );
        fos.close();
    }
    /**
     * Read in an anthology OR just specify where you want it to be
     * @param src the source anthology file
     * @param msrc the markup source, in same folder ideally
     * @throws Exception 
     */
    private void internalise() throws Exception
    {
        if ( src.exists() && msrc.exists() )
        {
            JSONDocument cortex = JSONDocument.internalise( src, "UTF-8" );
            this.title = (String)cortex.get( JSONKeys.TITLE );
            this.description = (String)cortex.get( JSONKeys.DESCRIPTION );
            STILDocument corcode = STILDocument.internalise( msrc );
            String body = (String)cortex.get( JSONKeys.BODY );
            String[] items = body.split("\n");
            int offset = 0;
            String key;
            Range r;
            for ( int i=0;i<items.length;i++ )
            {
                if ( i == 0 )
                    key = JSONKeys.TITLE;
                else if ( i==1 )
                    key = JSONKeys.DESCRIPTION;
                else
                    key = JSONKeys.ENTRY;
                r = corcode.get( key, offset, 
                    items[i].length()+1 );
                if ( r != null && r.name.equals(JSONKeys.DESCRIPTION) 
                    && !descriptionSet() )
                    description = items[i];
                offset += items[i].length()+1;
                if ( r != null && r.annotations != null 
                    && r.annotations.size()>0 )
                {
                    Annotation a = r.annotations.get( 0 );
                    addItem( items[i], (String)a.getValue() );
                }
            }
            Utils.removeDir( parent );
            // markup file will be writen out afresh by externalise
        }
        // not an error, just not already existing
    }
}
