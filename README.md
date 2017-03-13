# clockwork-electron

As presented at Clojure eXchange 2016

Find the slides in the `doc/` folder

Please note that the instructions below are OSX specific, please adapt (and feel free to send a PR) if you're on Linux or Windows.

## Requirements

* leiningen 2.6.x +
* node v0.12.x +
* grunt v0.1.13 +

### (if you don't install grunt yet.)

```
$ npm install -g grunt-cli
```


## Usage

### step 1

run `clockwork-init`


```
$ lein clockwork-init
 ...
 
Running "download-electron" task
 
Done, without errors.
```

### step 2

run cljsbuild 

```
lein clockwork-once
```

and run extern alias `clockwork-externs`,

```
$ lein clockwork-externs
```

### step 4

You can run the desktop application now.


#### development mode

development mode use figwheel. run alias `clockwork-figwheel`.  before run application.
Open other terminal window.

```
$ lein clockwork-figwheel
```

and you can run Electron(Atom-Shell) app.

```
$ ./node_modules/electron/dist/Electron.app/Contents/MacOS/Electron .out/dev
```

#### production mode

you can run Electron(Atom-Shell) app.

```
$ ./node_modules/electron/dist/Electron.app/Contents/MacOS/Electron app/prod
```

## Packaging for distribution

TBD

## Troubleshooting

The enviroment includes cljs-devtools with custom formatters. These should let you 
properly inspect cljs data structures from the devtool panel.
Make sure "Settings -> Preferences -> Console -> Enable custom formatters" is checked.

## License

Copyright © Riccardo Cambiassi

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
