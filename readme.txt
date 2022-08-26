............................. RC CUBE (For Neutron Star @FEB 16, 2001) ..............................

# A modular 3D N*N rubik's cube program with solver and command line control

# Usage

1. Install Java on your computer and add it to the path
2. Copy folder "out\artifacts\cube_jar" to your computer
2. click "launch.bat"

# Moves (Clockwise)

U: Up
R: Right
F: Front
D: Down
L: Left
B: Back

-> Moves are case insensitive
-> Add prime (') for anticlockwise move. ex R [clockwise] -> R' [anti-clockwise]

-> type move twice for a half turn (180 deg)
example: R [single turn] -> RR [double turn]

-> To turn middle slice, add it's index [0, n-1] from the side of move
example: 1. To turn 2nd slice from right -> R1
	      2. To turn 4th slice from UP anticlockwise -> U'3
 
-> To turn multiple slices in a single move, type their indices in brackets separated by comma
example: Turn 1st and 3rd slices from right in a 5*5 cube -> R[0,2]


# Commands

-> n [dimension] -> set cube dimension
-> scramble [moves]: scramble with given no of moves
-> solve: Solve/Apply solution (Only for 3*3 cube)
-> reset [what]: Reset [cube, zoom]
-> undo: undo last move
-> finish [c]: finish/cancel animating and pending moves
-> enter moves sequence (moves separated by space)
-> exit/quit: quit

# Demo

https://www.youtube.com/watch?v=Ivm3o5bG-fI
