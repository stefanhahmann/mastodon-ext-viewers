Welcome
-------
This is a repository with [Mastodon](https://github.com/mastodon-sc/mastodon) plugins
topically related to exporting lineage tree in various shapes, e.g.

- with all tracks spots decimated into one graph vertex,
- with straight lines (showing daughter-mother relation ship) between such vertices,
- with rectangularly bended lines,
- with various left-to-right display order of daughters,

and into various sinks, e.g.

- as `.graphml` file for [yEd](https://www.yworks.com/products/yed),
- in a separate window rendered with [GraphStream](https://graphstream-project.org/),
- in a separate applications such as [Blender](https://www.blender.org/) or [sciview](https://imagej.net/plugins/sciview).

It was developed and is maintained by [Vladimír Ulman](http://www.fi.muni.cz/~xulman/).
The BioMatch Blender addon ([see below](#blender)) was developed together
with Petr Strakoš from [IT4Innovation](https://www.it4i.cz/en).


License
--------
All here is licensed with the [BSD 2-Clause License](https://choosealicense.com/licenses/bsd-2-clause/).


Install by enabling respective Fiji update sites
------------------------------------------------
1. Open [Fiji](https://fiji.sc/)
1. Click menus: 'Help' -> 'Update...'
1. Click 'Manage update sites' in the opened 'ImageJ Updater' dialog
1. Mark the 'Mastodon' and 'TomancakLab' checkboxes
1. Click 'Close' to close the dialog


Notes
------
Once installed, one can find the tools in the Mastodon, in the _Plugins_ menu.
Contact (ulman při fi.muni.cz) for help on how to use it.

### Blender
This particular sink, besides its practical value, serves as an example
of foreign, network-connected application that can receive the lineage as
a set of drawing commands to have the lineage displayed. The connection is
achieved by utilization of the [grpc](https://grpc.io/) library that
transfers over [our own protocol](https://github.com/xulman/graphics-net-transfers/blob/master/protocol_specification/points_and_lines.proto),
which is easy to use and for which [demo servers and clients exists](https://github.com/xulman/graphics-net-transfers).

To have the Mastodon -> Blender connection functional, one needs to install

1. grpc library into Blender,
1. [Our DisplayServer addon](https://www.fi.muni.cz/~xulman/files/Mastodon/Blender/DisplayServer.zip) to Blender, 
1. Possibly even [our BioMatch addon](https://www.fi.muni.cz/~xulman/files/Mastodon/Blender/Biomatch.zip) to Blender.

Here's how to add grpc into your installed Blender (for 2.9x version series):

1. Stop & close Blender
1. Open a terminal (konsole)
1. `cd` into where you have dropped the Blender.app
1. Further `cd` deep into it:
	1. on mac: `cd Blender.app/Contents/Resources/2.93/python/bin`
	1. on linux: `cd blender-2.93.1-linux-x64/2.93/python/bin`
	1. on windows: ...similarily...
1. Run the following commands (one full line, one command, the same on all OSes):
	1. `./python3.9 -m ensurepip`
	1. `./python3.9 -m pip install --upgrade pip`
	1. `./pip install grpcio-tools`
1. Open Blender again


### sciview
This particular sink is also implemented via the network connection.
The client code is implemented here, in this repo.
The server side is implemented in [the allied other repository ;-)](https://github.com/mastodon-sc/mastodon-sciview).
