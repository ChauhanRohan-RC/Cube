//package main;
//
//public class Temp {
//
//    public static void main(String[] args) {
//
//        U.println(R.SHELL_INSTRUCTIONS);
//        Scanner sc;
//        boolean running = true;
//
//        do {
//            sc = new Scanner(System.in);
//            print(R.SHELL_ROOT);
//
//            final String in = sc.nextLine();
//            if (in.isEmpty())
//                continue;
//
//            if (in.startsWith("n")) {
//                final String rest = in.substring(1).strip();
//                final Runnable usagePrinter = () -> U.println("Usage: n <dimension>\nDimension should be in range [2, " + CubeI.DEFAULT_MAX_N + "]");
//
//                if (rest.isEmpty()) {
//                    usagePrinter.run();
//                    continue;
//                }
//
//                try {
//                    final int n = Integer.parseInt(rest);
//                    if (n < 2 || n > CubeI.DEFAULT_MAX_N) {
//                        u.printerrln(R.SHELL_DIMENSION + "Cube dimension must be >= 2 and <= " + CubeI.DEFAULT_MAX_N);
//                        usagePrinter.run();
//                    } else {
//                        app.enqueueTask(() -> app.cubeGL.setN(n, true /* use cli flag */));
//                    }
//                } catch (NumberFormatException ignored) {
//                    u.printerrln(R.SHELL_DIMENSION + "Cube dimension must be an integer, command: n [dim]");
//                    usagePrinter.run();
//                }
//            } else if (in.startsWith("scramble")) {
//                final String rest = in.substring(8).strip();
//                final Runnable usagePrinter = () -> U.println("Usage: scramble <num_moves>");
//                int n = CubeI.DEFAULT_SCRAMBLE_MOVES;
//
//                if (rest.isEmpty()) {
//                    U.println(R.SHELL_SCRAMBLE + "Using default scramble moves: " + n);
//                    usagePrinter.run();
//                } else {
//                    try {
//                        final int n2 = Integer.parseInt(rest);
//                        if (n2 <= 0) {
//                            U.w(R.SHELL_SCRAMBLE, "Scramble moves must be positive integer, falling back to default scramble moves: " + n);
//                            usagePrinter.run();
//                        } else {
//                            n = n2;
//                        }
//                    } catch (NumberFormatException ignored) {
//                        U.w(R.SHELL_SCRAMBLE, "Scramble moves must be positive integer, falling back to default scramble moves: " + n);
//                        usagePrinter.run();
//                    }
//                }
//
//                app.scramble(n);
//            } else if (in.startsWith("reset")) {
//                if (in.length() > 5 && in.substring(5).endsWith("zoom")) {
//                    app.resetCubeDrawScale();
//                } else {
//                    app.cubeGL.resetCube();
//                }
//            } else if (in.startsWith("finish")) {
//                app.cubeGL.finishAllMoves(in.endsWith("c"));
//            } else if (in.equals("solve")) {
//                app.solve();
//            } else if (in.equals("undo")) {
//                app.cubeGL.undoLastMove();
//            } else if (in.startsWith("speed")) {
//                final String rest = in.substring(5).strip();
//                final Runnable usagePrinter = () -> U.println("Usage: speed <option>\nAvailable options\n\t+ : Increase speed\n\t- : Decrease Speed\n\t<percent> : set speed percentage in range [0, 100]");
//
//                if (rest.isEmpty()) {
//                    U.println("Current Speed: " + Format.nf001(app.getMoveSpeedPercent()));
//                    usagePrinter.run();
//                    continue;
//                }
//
//                final float newPer;
//
//                if (rest.equals("+")) {
//                    newPer = app.incMoveSpeed(false);
//                } else if (rest.equals("-")) {
//                    newPer = app.decMoveSpeed(false);
//                } else {
//                    try {
//                        final float in_per = Float.parseFloat(rest);
//                        if (in_per < 0 || in_per > 100) {
//                            u.printerrln(R.SHELL_MOVE + "Move speed should be in range [0, 100], given: " + Format.nf001(in_per));
//                            usagePrinter.run();
//                            continue;
//                        }
//
//                        newPer = app.setMoveSpeedPercent(in_per);
//                    } catch (NumberFormatException ignored) {
//                        u.printerrln(R.SHELL_MOVE + "Move speed should be a number in range [0, 100], given: " + rest);
//                        usagePrinter.run();
//                        continue;
//                    }
//                }
//
//                U.println(R.SHELL_MOVE + "Move speed set to " + Format.nf001(newPer) + "%");
//            }
//
//            else if (in.startsWith("intp") || in.startsWith("interp") || in.startsWith("interpolator")) {
//                String key = "";
//                // checks with spaces
//                if (in.startsWith("intp ")) {
//                    key = in.substring(5);
//                } else if (in.startsWith("interp ")) {
//                    key = in.substring(7);
//                } else if (in.startsWith("interpolator "))  {
//                    key = in.substring(13);
//                }
//
//                final Runnable usagePrinter = () -> U.println("Usage: interpolator <interpolator_key>\nAvailable Interpolators (key -> name)\n" + InterpolatorInfo.getDisplayInfo());
//                if (key.isEmpty()) {
//                    usagePrinter.run();
//                    continue;
//                }
//
//                final InterpolatorInfo info = InterpolatorInfo.fromKey(key);
//                if (info == null) {
//                    u.printerrln(R.SHELL_MOVE + "Invalid interpolator key <" + key + ">");
//                    usagePrinter.run();
//                    continue;
//                }
//
//                final boolean changed = app.setMoveGlInterpolator(info.interpolator);
//                final String msg = changed? "Move animation interpolator changed to: " + info.displayName: "Move animation interpolator already set to: " + info.displayName;
//                U.w(R.SHELL_MOVE, msg);
//            }
//
//            else if (in.equals("quit") || in.equals("exit")) {
//                running = false;
//            } else {
//                try {
//                    List<Move> moves = Move.parseSequence(in);
//                    if (CollectionUtil.notEmpty(moves)) {
//                        app.applySequence(moves);
//                    }
//                } catch (Move.ParseException e) {
//                    u.printerrln(R.SHELL_MOVE + e.getMessage());
//                }
//            }
//        } while (running);
//
//        app.exit();
//    }
//
//}
