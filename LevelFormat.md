# Storage Format #

Each map is represented as a matrix of values with each element corresponding to a different tile. The map matrix is of size 100 x 100 and is stored using a column-major layout as a text string of characters. The upper-left most position in the map corresponds the the tile at index (0, 0).

Each character value, starting with 'a' equivalent to 0, in the level string corresponds to a tile from the tiles sprite images (see tiles\_0.png and tiles\_1.png in [sprite resources](http://code.google.com/p/alienbloodbath/source/browse/#svn/trunk/res/drawable) for examples). A value of 'a' corresponds to the top most tile in the tiles image (blank open space) where as, for example, 'c' corresponds to the 3rd tile from the top.

# Adding new Levels to the Game #

New levels may be added to the SD card storage device under a directory named "abb\_maps". Within the "abb\_maps" directory, each sub-directory represents a unique map. The tiles are stored within the map directory as "tiles.txt" and the file is expected to contain the consecutive ASCII characters for the map matrix elements as described above.

**Optionally**, each map directory may contain a "tiles.png" file which overrides the default game map tile images. The tile images is expected to contain tiles of the same size and the tile semantics (eg. spikes = death) do not change. See above tile image examples to use as templates.

Example directory structure:
```
sdcard/abb_maps/my_level/tiles.txt
sdcard/abb_maps/my_level_with_new_tiles/tiles.txt
sdcard/abb_maps/my_level_with_new_tiles/tiles.png
sdcard/abb_maps/my_level_with_new_tiles/tile_effects.txt
```

## Special Tile Values ##

Special tile values are used to mark certain aspects of the game including level start and end locations as well as enemy locations.

| **Tile Value** | **Meaning**             |
|:---------------|:------------------------|
| 9              | Tile explodes.          |
| 4              | Tile causes death.      |
| 10             | Map start.              |
| 11             | Map end.                |
| 12             | Enemy spawn location.   |

The following tile values are collidable:
1-5, and 7.

**Optionally**, special tiles may be modified. The tile id semantics may be overridden by providing a "tile\_effects.txt" file within the map directory. The expected format is an ASCII file with two values per line. Each line must first specify the integer tile index and then the effect name / meaning. The tile effect may be one of either: solid, death, or explode.

Example tile\_effects.txt file:
```
1 solid
2 solid
3 solid
4 death
7 solid
9 explode
```