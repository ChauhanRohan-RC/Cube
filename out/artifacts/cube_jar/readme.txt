
............................. 3D N*N Rubik's Cube AI ..............................
This is a 3D Rubik's cube visualizer and solver program. It supports any N*N*N dimension cube, with UI and Command-Line controls

# MOVES (Clockwise)
  U: Up
  R: Right
  F: Front
  D: Down
  L: Left
  B: Back

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

# COMMANDS
 -> n [dimension] -> set cube dimension
 -> scramble [moves]: scramble with given no of moves
 -> solve: Solve/Apply solution (Only for 3*3 cube)
 -> reset [what]: Reset [cube, zoom]
 -> undo: undo last move
 -> finish [c]: finish/cancel animating and pending moves
 -> enter moves sequence (moves separated by space)
 -> exit/quit: quit
