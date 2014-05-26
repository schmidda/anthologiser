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
 * Choose which folder to create
 * @author desmond
 */
public class FolderFactory 
{
    static Folder makeFolder( File src, File dst, Format format ) throws Exception
    {
        dst = new File( dst, format.toString() );
        switch ( format )
        {
            case MVD:
                return new MVDFolder( src, dst );
            case TEXT:
                return new TextFolder( src, dst );
            case XML:
                return new XMLFolder( src, dst );
        }
        // only used if format is null
        return null;
    }
}
