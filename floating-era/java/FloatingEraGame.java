import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class FloatingEraGame extends JFrame {
    public FloatingEraGame() {
        setTitle("浮空纪元 - Floating Era");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        GamePanel gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        JPanel sidebar = createSidebar(gamePanel);
        add(sidebar, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createSidebar(GamePanel game) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(10, 20, 40));
        panel.setBorder(BorderFactory.createLineBorder(new Color(74, 144, 217), 2));
        panel.setPreferredSize(new Dimension(280, 700));

        JLabel title = new JLabel("浮空纪元", SwingConstants.CENTER);
        title.setForeground(new Color(110, 200, 255));
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel weatherLabel = new JLabel("天气: 晴朗", SwingConstants.CENTER);
        weatherLabel.setForeground(new Color(160, 200, 255));
        weatherLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        weatherLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(weatherLabel);
        game.setWeatherLabel(weatherLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        panel.add(createSection("飞艇状态"));
        JLabel posLabel = createStatLabel("坐标: 0, 0");
        JLabel spdLabel = createStatLabel("速度: 0");
        JLabel cargoLabel = createStatLabel("货仓: 0/50");
        JLabel hpLabel = createStatLabel("船体: 100/100");
        JLabel atkLabel = createStatLabel("战斗力: 10");
        panel.add(posLabel); panel.add(spdLabel); panel.add(cargoLabel);
        panel.add(hpLabel); panel.add(atkLabel);
        game.setStatLabels(posLabel, spdLabel, cargoLabel, hpLabel, atkLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        panel.add(createSection("资源"));
        JLabel woodLabel = createStatLabel("木材: 0");
        JLabel oreLabel = createStatLabel("矿石: 0");
        JLabel techLabel = createStatLabel("科技碎片: 0");
        JLabel energyLabel = createStatLabel("能源: 100");
        panel.add(woodLabel); panel.add(oreLabel); panel.add(techLabel); panel.add(energyLabel);
        game.setResourceLabels(woodLabel, oreLabel, techLabel, energyLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        panel.add(createSection("建造与升级"));
        panel.add(createUpgradeButton("扩建货仓(+20) - 木材20", e -> game.upgrade("cargo")));
        panel.add(createUpgradeButton("强化装甲 - 矿石15", e -> game.upgrade("armor")));
        panel.add(createUpgradeButton("火力升级 - 矿石20 科技5", e -> game.upgrade("weapon")));
        panel.add(createUpgradeButton("引擎改造 - 木材15 科技5", e -> game.upgrade("engine")));
        panel.add(createUpgradeButton("建设前哨站 - 木材50 矿石30", e -> game.upgrade("outpost")));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        panel.add(createSection("日志"));
        JTextArea logArea = new JTextArea(8, 20);
        logArea.setEditable(false);
        logArea.setBackground(new Color(5, 15, 30));
        logArea.setForeground(new Color(160, 184, 208));
        logArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setPreferredSize(new Dimension(260, 120));
        panel.add(scroll);
        game.setLogArea(logArea);

        JLabel controls = new JLabel("WASD移动 | E采集 | 空格攻击");
        controls.setForeground(new Color(100, 130, 170));
        controls.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        controls.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(controls);

        return panel;
    }

    private JLabel createSection(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(160, 200, 255));
        label.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(74, 144, 217)));
        label.setMaximumSize(new Dimension(260, 25));
        return label;
    }

    private JLabel createStatLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(200, 220, 255));
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        label.setMaximumSize(new Dimension(260, 20));
        return label;
    }

    private JButton createUpgradeButton(String text, ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        btn.setBackground(new Color(42, 90, 158));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(260, 28));
        btn.addActionListener(listener);
        return btn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FloatingEraGame::new);
    }
}
