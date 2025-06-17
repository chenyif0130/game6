import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class GameScanner {    //  游戏扫描器
    private static Point startPoint;//  鼠标起始位置
    private static Rectangle selectedArea;//  游戏区域
    private static final GemTypes[][] grid = new GemTypes[8][8]; // 存储当前棋盘状态
    private static long lastSwapTime = 0;
    private static final int SWAP_COOLDOWN_MS = 200; // 冷却时间：500 毫秒

    public static void captureGameArea() { //  截取游戏区域
        JFrame overlay = new JFrame();//  创建一个半透明的窗口
        overlay.setUndecorated(true);//  设置窗口透明
        overlay.setAlwaysOnTop(true);// 设置窗口置顶
        overlay.setOpacity(0.3f); // 半透明
        overlay.setBackground(new Color(0, 0, 0, 50));
        overlay.setExtendedState(JFrame.MAXIMIZED_BOTH);
        overlay.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        JPanel panel = new JPanel() {
            Point dragStart = null;
            Point dragEnd = null;

            {
                addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        dragStart = e.getPoint();
                    }

                    public void mouseReleased(MouseEvent e) {
                        dragEnd = e.getPoint();
                        int x = Math.min(dragStart.x, dragEnd.x);
                        int y = Math.min(dragStart.y, dragEnd.y);
                        int width = Math.abs(dragStart.x - dragEnd.x);
                        int height = Math.abs(dragStart.y - dragEnd.y);

                        selectedArea = new Rectangle(x, y, width, height);
                        Main.gameArea = selectedArea;

                        overlay.dispose();
                        JOptionPane.showMessageDialog(null, "窗口范围已设置：" + selectedArea);
                    }
                });

                addMouseMotionListener(new MouseMotionAdapter() {
                    public void mouseDragged(MouseEvent e) {
                        dragEnd = e.getPoint();
                        repaint();
                    }
                });
            }


            protected void paintComponent(Graphics g) {// 绘制游戏区域
                super.paintComponent(g);// 调用父类的绘制方法
                if (dragStart != null && dragEnd != null) {
                    int x = Math.min(dragStart.x, dragEnd.x);
                    int y = Math.min(dragStart.y, dragEnd.y);
                    int width = Math.abs(dragStart.x - dragEnd.x);
                    int height = Math.abs(dragStart.y - dragEnd.y);

                    g.setColor(Color.RED);
                    ((Graphics2D) g).setStroke(new BasicStroke(2));
                    g.drawRect(x, y, width, height);
                }
            }
        };

        overlay.setContentPane(panel);
        overlay.setVisible(true);
    }

    public static void startScanning() {
        try {
            Robot robot = new Robot();
            while (Main.running) {
                if (Main.gameArea != null) {
                    BufferedImage img = robot.createScreenCapture(Main.gameArea);
                    analyzeGrid(img);
                    autoSwap(); // 自动交换逻辑
                }
                Thread.sleep(50); // 控制扫描频率
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void analyzeGrid(BufferedImage img) {
        int rows = 8, cols = 8;
        int cellW = img.getWidth() / cols;
        int cellH = img.getHeight() / rows;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int cx = x * cellW + cellW / 2;
                int cy = y * cellH + cellH / 2;
                Color c = new Color(img.getRGB(cx, cy));
                GemTypes colorName = classifyColor(c);
                grid[y][x] = colorName; // 将识别结果存储到二维数组中
                System.out.print(colorName + "\t");
            }
            System.out.println();
        }
    }


    private static void autoSwap() {
        if (System.currentTimeMillis() - lastSwapTime < SWAP_COOLDOWN_MS) {
            return; // 冷却中
        }

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (x + 1 < 8 && isValidSwap(x, y, x + 1, y)) {
                    performSwap(x, y, x + 1, y);
                    lastSwapTime = System.currentTimeMillis(); // 记录交换时间
                    return;
                }
                if (y + 1 < 8 && isValidSwap(x, y, x, y + 1)) {
                    performSwap(x, y, x, y + 1);
                    lastSwapTime = System.currentTimeMillis(); // 记录交换时间
                    return;
                }
            }
        }
    }

    private static boolean isValidSwap(int x1, int y1, int x2, int y2) {
        swapGrid(x1, y1, x2, y2);
        boolean result = causesMatch(x1, y1) || causesMatch(x2, y2);
        swapGrid(x1, y1, x2, y2); // 还原
        return result;
    }

    private static void swapGrid(int x1, int y1, int x2, int y2) {
        GemTypes temp = grid[y1][x1];
        grid[y1][x1] = grid[y2][x2];
        grid[y2][x2] = temp;
    }

    private static boolean causesMatch(int x, int y) {
        GemTypes gem = grid[y][x];
        if (gem == GemTypes.Unknown) return false;

        // 横向统计
        int count = 1;
        for (int i = x - 1; i >= 0 && grid[y][i] == gem; i--) count++;
        for (int i = x + 1; i < 8 && grid[y][i] == gem; i++) count++;
        if (count >= 3) return true;

        // 纵向统计
        count = 1;
        for (int i = y - 1; i >= 0 && grid[i][x] == gem; i--) count++;
        for (int i = y + 1; i < 8 && grid[i][x] == gem; i++) count++;
        return count >= 3;
    }


    private static boolean hasMatch(GemTypes[][] g) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                // 检查横向三连
                if (x + 2 < 8 && g[y][x] == g[y][x + 1] && g[y][x] == g[y][x + 2]) {
                    return true;
                }
                // 检查纵向三连
                if (y + 2 < 8 && g[y][x] == g[y + 1][x] && g[y][x] == g[y + 2][x]) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void performSwap(int x1, int y1, int x2, int y2) {
        try {
            Robot robot = new Robot();
            // 获取屏幕坐标
            int startX = (int) (Main.gameArea.x + (x1 + 0.5) * Main.gameArea.width / 8);
            int startY = (int) (Main.gameArea.y + (y1 + 0.5) * Main.gameArea.height / 8);
            int endX = (int) (Main.gameArea.x + (x2 + 0.5) * Main.gameArea.width / 8);
            int endY = (int) (Main.gameArea.y + (y2 + 0.5) * Main.gameArea.height / 8);

            // 模拟鼠标点击和拖拽
            /*robot.mouseMove(startX, startY);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseMove(endX, endY);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);*/
            robot.mouseMove(startX, startY);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

            robot.mouseMove(endX, endY);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

            System.out.println("交换 (" + x1 + ", " + y1 + ") 和 (" + x2 + ", " + y2 + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public enum GemTypes {
        White,
        Red,
        Orange,
        Yellow,
        Green,
        Blue,
        Pink,
        Unknown
    }

    private static String getNumberFromColorComp(int value) {
        if (value < 90) {
            return "1";
        } else if (value < 192) {
            return "2";
        } else {
            return "3";
        }
    }

    private static GemTypes classifyColor(Color c) {
        String identifierString = "";
        identifierString += getNumberFromColorComp(c.getRed());
        identifierString += getNumberFromColorComp(c.getGreen());
        identifierString += getNumberFromColorComp(c.getBlue());

        GemTypes resultGem = GemTypes.Unknown;

        switch (identifierString) {
            case "333":
                resultGem = GemTypes.White;
                break;
            case "311":
                resultGem = GemTypes.Red;
                break;
            case "332":
                resultGem = GemTypes.Orange;
                break;
            case "331":
                resultGem = GemTypes.Yellow;
                break;
            case "232":
            case "132":
                resultGem = GemTypes.Green;
                break;
            case "123":
                resultGem = GemTypes.Blue;
                break;
            case "313":
            case "212":
            case "312":
            case "213":
                resultGem = GemTypes.Pink;
                break;
        }

        return resultGem;
    }
}
