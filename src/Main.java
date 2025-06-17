import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.*;

public class Main {
    public static Rectangle gameArea = null;
    public static volatile boolean running = false;
    private static ExecutorService executor;

    public static void main(String[] args) {
        // 创建主窗口
        JFrame frame = new JFrame("宝石迷阵自动操作");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLayout(new GridLayout(3, 1));

        // 创建三个按钮
        JButton selectBtn = new JButton("选择游戏窗口范围");
        JButton startBtn = new JButton("开始游戏");
        JButton stopBtn = new JButton("结束游戏");

        frame.add(selectBtn);
        frame.add(startBtn);
        frame.add(stopBtn);

        // 添加按钮事件
        selectBtn.addActionListener(e -> GameScanner.captureGameArea());
        startBtn.addActionListener(e -> {
            if (gameArea != null && !running) {
                running = true;
                executor = Executors.newFixedThreadPool(2);
                executor.submit(GameScanner::startScanning);
            }
        });
        stopBtn.addActionListener(e -> stopProgram());

        // 注册 Ctrl + F5 快捷键（仅在窗口激活时有效）
        registerShortcut(frame);

        frame.setVisible(true);
    }

    public static void stopProgram() {
        running = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        System.out.println("游戏自动操作已结束");
        System.exit(0);
    }

    private static void registerShortcut(JFrame frame) {
        JRootPane rootPane = frame.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.CTRL_DOWN_MASK);
        inputMap.put(keyStroke, "stop");
        actionMap.put("stop", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopProgram();
            }
        });
    }
}
