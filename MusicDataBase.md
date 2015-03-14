# Introduction #
To avoid reading tags from music files when playing it, we have created a database where they are stored. When songs are added to the player library they are read and tags extracted. Then you can quickly and easily get informations about the songs in your library.

# Database scheme #

SONG(_id, url, title, last\_played, count\_played, track, #artist, #album, #genre)_

ARTIST(id, name)

ALBUM(id, name, url\_cover)

GENRE(id, name)

PLAYLIST(pos, #id)  (id is associated with _id from SONG)_

# Foreign Key #
The foreign keys are managed with triggers but considering the possibilities of SQLite they are also managed within the code when inserting.

# Database Connection #
It is achieved by the class MusicDB which allows to create a database if necessary or to connect to an existing one. This class comes with several methods to insert, delete and retrieve information. Normally, all the SQL code has to be in this class and MusicDB abstracts the database.

# Songs meta data #
The MusicDB class is also in charge of extracting the meta data from each music files.
For now, the ID3 tags are read with ID3TagReader class (ID3v1 & ID3v2 tags are supported).