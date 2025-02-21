=======================  Rubik's Cube Simulation and Solver  =======================
This is a 3D NxNxN Rubik's cube simulation and solver program. It supports any N-dimensional cube, with both Graphical and Command-Line control interfaces

## HOW TO RUN?
-> Install java on your computer and add it to PATH
-> Open cmd/terminal and run "java -jar cube.jar"

## MOVES NOTATION ----------------------------------------------

 U : Up
 R : Right
 F : Front
 D : Down
 L : Left
 B : Back

-> Moves are case insensitive
-> Add prime (') for anticlockwise move.
    ex R [clockwise] -> R' [anti-clockwise]
-> type move twice for a half turn (180 deg)
    ex R [single turn] -> RR [double turn]
-> To turn middle slice, add it's index in range [0, n-1] from the side of move
    ex 1. To turn 2nd slice from right -> R1
       2. To turn 4th slice from UP anticlockwise -> U'3
-> To turn multiple slices in a single move, type their indices in brackets separated by comma
    ex Turn 1st and 3rd slices from right in a 5*5 cube -> R[0,2]   


## UI CONTROLS ----------------------------------------------------

-> Escape : Cancel  [Discrete]
	Cancel pending moves and stop solving, or exit.

-> [Ctr]-[Shf]-[U R F D L B] : Moves  [Discrete]
	Cube moves.
	<Keys> : [U | R | F | D | L | B] keys -> Clockwise Move
	         with Shift -> Anticlockwise Move
	         with Ctrl -> 2-Slice Move
	         with Ctrl-Shift -> 180° Move

-> [Ctr | Shf]-N : Cube  [Discrete]
	Change cube Dimension.
	<Keys> : N -> Increase Size  |  Shift-N -> Decrease Size  |  Ctrl-[Shift]-N -> Force change size

-> Space : Scramble  [Discrete]
	Scramble with default number of moves (default: 30).

-> Enter : Solve  [Discrete]
	Solve or apply solution.

-> [Ctr | Shf]-Q : Reset  [Discrete]
	Resets the Cube.
	<Keys> : Q -> Reset Cube State
	         Shift-Q -> Reset Camera
	         Ctrl-Q -> Reset Simulation
	         Ctrl-Shift-Q -> Reset Everything

-> Ctr-Z : Undo Last  [Discrete]
	Undo last move

-> X : Finish Moves  [Discrete]
	Finish all running and pending moves

-> Shf-X : Cancel Moves  [Discrete]
	Cancel all running and pending moves

-> A : Move Anim  [Discrete]
	Toggle Move Animations.

-> [Shf]-/ : Speed  [Continuous]
	Move animation speed (in percent) and animation time (in ms).
	<Keys> : / -> Increase Speed  |  Shift-/ -> Decrease Speed

-> I : Interp  [Discrete]
	Change Move Animation Interpolator.

-> S : Sound  [Discrete]
	Toggle Sounds.

-> Shf-S : Poly Rhythm  [Discrete]
	Toggle Poly Rhythm (play multiple notes at once).

-> H : HUD  [Discrete]
	Show/Hide HUD.

-> C : Controls  [Discrete]
	Show/Hide Control Key Bindings.

-> W : Window  [Discrete]
	Sets the fullscreen mode to Expanded or Windowed.

-> Shf-A : Axes  [Discrete]
	Show / Hide cube axes.

-> Ctr-S : Save Frame  [Discrete]
	Save Current graphics frame in a png file.

-> [Shf]-Z : Zoom  [Discrete]
	Cube Zoom, in both multiples and percentage.
	<Keys> : Z -> Zoom-In  |  Shift-Z -> Zoom-Out

-> V : Camera  [Discrete]
	Toggle camera mode between FREE and LOCKED.

-> Up/Down : Pitch-X  [Discrete]
	Controls the Camera PITCH (rotation about X-Axis).
	<Keys> : [UP | DOWN] arrow keys

-> Left/Right : Yaw-Y  [Discrete]
	Controls the Camera YAW (rotation about Y-Axis).
	<Keys> : [LEFT | RIGHT] arrow keys

-> Shf-Left/Right : Roll-Z  [Discrete]
	Controls the Camera ROLL (rotation about Z-Axis).
	<Keys> : Shift-[LEFT | RIGHT] arrow keys


## COMMANDS ----------------------------------------------------

-> help [-moves | -controls | -commands | -all] : Print usage information
   Scopes
   1. -moves -> print moves notation and usage
   1. -controls -> print controls usage
   2. -commands -> print commands usage
   3. -all -> print entire usage information

-> <move_seq> : Directly enter moves separated by space to apply
   Example: U R' FF B2 L'[0,1]

-> size <size> : Sets the cube size
   Alias: dim, cube

-> solve : Solve the current state or Apply solution
-> scramble <num_moves> : Scramble with the given number of moves

-> undo : Undo the last move
-> finish : finish animating and pending moves
-> cancel : cancel solver and all pending moves
   Alias: stop

-> reset [-f] [-state | -env | -cam | -win | -all] : Reset given scope(s)
   Scopes
   1. -state -> reset cube state
   2. -env -> reset simulation environment
   3. -cam -> reset camera (pitch, yaw and roll)
   4. -win -> reset window size and position
   5. -all -> reset everything  (Default)
   Options
   1. -f -> force reset without animations

-> save : Save current frame to a png file
   Alias: snap, snapshot, saveframe

-> axes : toggle cube axes
   Alias: toggle axes

-> anim : toggle move animations
   Alias: toggle anim, animations, toggle animations

-> speed [-p | -d] <value> : Set move animation speed or duration
   Modes
   1. -p -> animation speed, in percentage  (Default)
   2. -d -> animation duration, in milliseconds

-> interpolator <next | key> : Sets the move animation interpolator
   Alias: intp, interp
   Wildcard: next -> cycle to next interpolator
   Keys
   1. default -> the default interpolator
   2. linear -> Linear interpolator
   3. bounce -> Bounce interpolator
   4. acc -> Accelerate interpolator
   5. dec -> Decelerate interpolator
   6. acd -> Accelerate-Decelerate interpolator
   7. anticipate -> Anticipate interpolator
   8. overshoot -> Overshoot interpolator

-> sound : Toggle sounds
-> poly-rhythm : Toggle Poly Rhythm mode. If enabled, it allows playing multiple notes at once

-> hud : Show / Hide HUD overlay
-> keys : Show / Hide control key bindings
   Alias: toggle keys, controls, toggle controls

-> win [-size | -pos] <x> <y> : Sets the window size or location on screen
   Options
   1. -size -> set window size.
   2. -pos -> set window location on screen
   Wildcards
   1. w : set to initial windowed size. To be used with -size option
   2. c : center window on screen. To be used with -pos option

-> expand : Expand / Collapse Fullscreen window
   Alias: toggle expand, collapse, toggle collapse

-> cam [-free | -locked | -toggle] : set or toggle camera modes
   Modes
   1. -free -> free camera mode, mouse controlled
   2. -locked -> locked camera, keyboard controlled
   3. -toggle -> toggle camera mode between FREE and LOCKED  (Default)
   Options
   1. -f -> force without animations

-> zoom [-x | -p] <value> : sets the cube zoom
   Alias: scale
   Modes
   1. -x -> Multiples or times  (Default)
   2. -p -> percentage, in range [0, 100]

-> pitch [-by | -f] <+ | - | value_in_deg> : Sets the camera pitch (rotation about X-axis)
   Alias: rx, rotx, rotationx
   Wildcards: + or up, - or down
   Options
   1. -by -> change current pitch by the given value
   2. -f -> force without animations

-> yaw [-by | -f] <+ | - | value_in_deg> : Sets the camera yaw (rotation about Y-axis)
   Alias: ry, roty, rotationy
   Wildcards: + or left, - or right
   Options
   1. -by -> change current yaw by the given value
   2. -f -> force without animations

-> roll [-by | -f] <+ | - | value_in_deg> : Sets the camera roll (rotation about Z-axis)
   Alias: rz, rotz, rotationz
   Wildcards: + or left, - or right
   Options
   1. -by -> change current roll by the given value
   2. -f -> force without animations

-> exit / quit: quit   
