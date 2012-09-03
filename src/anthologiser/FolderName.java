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
import java.io.File;
/**
 * Create a unique folder name
 * @author desmond
 */
public class FolderName 
{
    String start;
    String end;
    File parent;
    FolderName( File parent )
    {
        this.parent = parent;
    }
    public void add( String name )
    {
        if ( start == null )
            start = name.toUpperCase();
        end = name.toUpperCase();
    }
    /**
     * Build the File object
     */
    public File compose()
    {
        if ( start.equals(end) )
            return new File( parent, start );
        else
        {
            int len = start.length();
            if ( end.length() < start.length() )
                len = end.length();
            for ( int i=0;i<len;i++ )
            {
                if ( start.charAt(i) != end.charAt(i) )
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append( start.substring(0,i+1) );
                    sb.append( "-" );
                    sb.append( end.substring(0,i+1) );
                    return new File( parent, sb.toString() );
                }
            }
            StringBuilder sb = new StringBuilder();
            if ( start.length()<end.length() )
            {
                sb.append( start );
                sb.append("-");
                sb.append(end.substring(0,start.length()+1) );
            }
            else
            {
                sb.append(start.substring(0,start.length()+1) );
                sb.append("-");
                sb.append( end );
            }
            return new File( parent, sb.toString() );
        }
    }
}
