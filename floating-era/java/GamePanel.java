import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class GamePanel extends JPanel implements Runnable {
    private Thread gameThread;
    private boolean running = true;
    private int W = 900, H = 700;

    private double camX = 0, camY = 0;
    private Player player = new Player();
    private java.util.List<Island> islands = new ArrayList<>();
    private java.util.List<Enemy> enemies = new ArrayList<>();
    private java.util.List<Bullet> bullets = new ArrayList<>();
    private java.util.List<Particle> particles = new ArrayList<>();
    private java.util.List<Cloud> clouds = new ArrayList<>();
    private java.util.List<Outpost> outposts = new ArrayList<>();

    private String weather = "clear";
    private int weatherTimer = 0;
    private Island nearbyIsland = null;
    private Random rand = new Random();

    private boolean[] keys = new boolean[256];

    private JLabel weatherLabel, posLabel, spdLabel, cargoLabel, hpLabel, atkLabel;
    private JLabel woodLabel, oreLabel, techLabel, energyLabel;
    private JTextArea logArea;

    public GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setBackground(new Color(11, 26, 58));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) { keys[e.getKeyCode()] = true; }
            public void keyReleased(KeyEvent e) { keys[e.getKeyCode()] = false; }
        });
        generateWorld();
        log("欢迎来到浮空纪元！驾驶飞艇探索云海中的岛屿吧。", Color.GREEN);
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void setWeatherLabel(JLabel label) { this.weatherLabel = label; }
    public void setStatLabels(JLabel pos, JLabel spd, JLabel cargo, JLabel hp, JLabel atk) {
        this.posLabel = pos; this.spdLabel = spd; this.cargoLabel = cargo;
        this.hpLabel = hp; this.atkLabel = atk;
    }
    public void setResourceLabels(JLabel wood, JLabel ore, JLabel tech, JLabel energy) {
        this.woodLabel = wood; this.oreLabel = ore; this.techLabel = tech; this.energyLabel = energy;
    }
    public void setLogArea(JTextArea area) { this.logArea = area; }

    private void generateWorld() {
        String[] types = {"forest", "mine", "ruin", "abandoned"};
        for (int i = 0; i < 50; i++) {
            String type = types[rand.nextInt(types.length)];
            Island island = new Island(rand.nextDouble() * 6000 - 3000, rand.nextDouble() * 6000 - 3000,
                40 + rand.nextDouble() * 50, type, 20 + rand.nextInt(60), generateName(type));
            boolean overlap = false;
            for (Island other : islands) {
                if (island.distanceTo(other) < island.r + other.r + 50) overlap = true;
            }
            if (!overlap) islands.add(island);
        }
        for (int i = 0; i < 30; i++) {
            clouds.add(new Cloud(rand.nextDouble() * 8000 - 4000, rand.nextDouble() * 8000 - 4000,
                60 + rand.nextDouble() * 140, 0.1 + rand.nextDouble() * 0.3, 0.15 + rand.nextDouble() * 0.2));
        }
        for (int i = 0; i < 12; i++) {
            double ex = rand.nextDouble() * 5000 - 2500;
            double ey = rand.nextDouble() * 5000 - 2500;
            enemies.add(new Enemy(ex, ey, 30, 5, rand.nextDouble() * Math.PI * 2, 0.8 + rand.nextDouble() * 0.7));
        }
    }

    private String generateName(String type) {
        String[] prefixes = {"碎云", "浮光", "遗迹", "风暴", "寂静", "琥珀", "星尘", "遗忘", "晨曦", "暮光"};
        java.util.Map<String, String> suffixes = new HashMap<>();
        suffixes.put("forest", "林岛"); suffixes.put("mine", "矿屿");
        suffixes.put("ruin", "废都"); suffixes.put("abandoned", "荒岛");
        return prefixes[rand.nextInt(prefixes.length)] + suffixes.get(type);
    }

    public void upgrade(String type) {
        if (type.equals("cargo")) {
            if (player.wood >= 20) { player.wood -= 20; player.maxCargo += 20; log("货仓扩建完成！容量 +20", Color.GREEN); }
            else log("木材不足！", Color.YELLOW);
        } else if (type.equals("armor")) {
            if (player.ore >= 15) { player.ore -= 15; player.maxHp += 30; player.hp += 30; log("装甲强化完成！船体 +30", Color.GREEN); }
            else log("矿石不足！", Color.YELLOW);
        } else if (type.equals("weapon")) {
            if (player.ore >= 20 && player.tech >= 5) { player.ore -= 20; player.tech -= 5; player.atk += 8; log("火力升级完成！攻击 +8", Color.GREEN); }
            else log("资源不足！", Color.YELLOW);
        } else if (type.equals("engine")) {
            if (player.wood >= 15 && player.tech >= 5) { player.wood -= 15; player.tech -= 5; player.speed += 0.8; log("引擎改造完成！速度提升", Color.GREEN); }
            else log("资源不足！", Color.YELLOW);
        } else if (type.equals("outpost")) {
            if (player.wood >= 50 && player.ore >= 30) {
                if (nearbyIsland != null) {
                    player.wood -= 50; player.ore -= 30;
                    outposts.add(new Outpost(nearbyIsland.x, nearbyIsland.y));
                    log("在 " + nearbyIsland.name + " 建立了前哨站！", Color.GREEN);
                } else log("必须靠近岛屿才能建立前哨站！", Color.YELLOW);
            } else log("资源不足！", Color.YELLOW);
        }
    }

    private void log(String msg, Color color) {
        if (logArea != null) {
            String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            logArea.append("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }
    private void log(String msg) { log(msg, new Color(160, 184, 208)); }

    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60;
        while (running) {
            long now = System.nanoTime();
            while (now - lastTime >= nsPerTick) {
                update();
                lastTime += nsPerTick;
            }
            repaint();
            try { Thread.sleep(2); } catch (InterruptedException e) { break; }
        }
    }

    private void update() {
        weatherTimer++;
        if (weatherTimer > 1200) {
            weatherTimer = 0;
            String[] weathers = {"clear", "windy", "storm", "fog"};
            weather = weathers[rand.nextInt(weathers.length)];
            java.util.Map<String, String> names = new HashMap<>();
            names.put("clear", "晴朗"); names.put("windy", "强风"); names.put("storm", "雷暴"); names.put("fog", "浓雾");
            java.util.Map<String, String> effects = new HashMap<>();
            effects.put("clear", "无影响"); effects.put("windy", "速度+30%"); effects.put("storm", "速度-40%"); effects.put("fog", "视野受限");
            log("天气变化：" + names.get(weather), Color.YELLOW);
            if (weatherLabel != null) weatherLabel.setText("天气: " + names.get(weather) + " | 影响: " + effects.get(weather));
        }

        double moveSpeed = player.speed;
        if (weather.equals("windy")) moveSpeed *= 1.3;
        if (weather.equals("storm")) moveSpeed *= 0.6;
        if (weather.equals("fog")) moveSpeed *= 0.8;

        double ax = 0, ay = 0;
        if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP]) ay -= 1;
        if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN]) ay += 1;
        if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT]) ax -= 1;
        if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT]) ax += 1;
        if (ax != 0 || ay != 0) {
            double len = Math.hypot(ax, ay);
            ax /= len; ay /= len;
            player.vx += ax * 0.3;
            player.vy += ay * 0.3;
            player.angle = Math.atan2(ay, ax);
        }
        player.vx *= 0.92;
        player.vy *= 0.92;
        player.x += player.vx * moveSpeed;
        player.y += player.vy * moveSpeed;

        if (keys[KeyEvent.VK_SPACE] && player.energy > 0) {
            fireBullet();
            player.energy -= 2;
            if (player.energy < 0) player.energy = 0;
        }
        if (player.energy < 100) player.energy += 0.1;

        nearbyIsland = null;
        for (Island island : islands) {
            double d = Math.hypot(player.x - island.x, player.y - island.y);
            if (d < island.r + 60) {
                nearbyIsland = island;
                if (keys[KeyEvent.VK_E] && island.resources > 0 && player.cargo < player.maxCargo) {
                    int amt = Math.min(3, Math.min(island.resources, player.maxCargo - player.cargo));
                    island.resources -= amt;
                    player.cargo += amt;
                    if (island.type.equals("forest")) player.wood += amt;
                    else if (island.type.equals("mine")) player.ore += amt;
                    else if (island.type.equals("ruin")) player.tech += (int) Math.ceil(amt / 2.0);
                    else { player.wood += (int) Math.ceil(amt / 2.0); player.ore += amt / 2; }
                    particles.add(new Particle(island.x + rand.nextDouble() * island.r * 2 - island.r,
                        island.y + rand.nextDouble() * island.r * 2 - island.r,
                        rand.nextDouble() * 2 - 1, -rand.nextDouble() * 1.5 - 0.5, 30, Color.ORANGE));
                    if (rand.nextDouble() < 0.1) log("从 " + island.name + " 采集了资源");
                    if (island.resources <= 0) log(island.name + " 的资源已枯竭...", Color.YELLOW);
                }
            }
        }

        for (Enemy e : enemies) {
            double d = Math.hypot(player.x - e.x, player.y - e.y);
            if (d < 300) {
                e.state = "chase";
                e.angle = Math.atan2(player.y - e.y, player.x - e.x);
            } else {
                e.state = "patrol";
                if (rand.nextDouble() < 0.02) e.angle += rand.nextDouble() * 2 - 1;
            }
            double sp = e.state.equals("chase") ? e.speed * 1.2 : e.speed;
            e.x += Math.cos(e.angle) * sp;
            e.y += Math.sin(e.angle) * sp;
            if (d < 40) {
                player.hp -= e.atk * 0.05;
                if (rand.nextDouble() < 0.05) log("受到攻击！船体受损", Color.YELLOW);
            }
        }
        enemies.removeIf(e -> e.hp <= 0);

        for (Bullet b : bullets) {
            b.x += Math.cos(b.angle) * 8;
            b.y += Math.sin(b.angle) * 8;
            b.life--;
            for (Enemy e : enemies) {
                if (Math.hypot(b.x - e.x, b.y - e.y) < 25) {
                    e.hp -= player.atk;
                    b.life = 0;
                    particles.add(new Particle(e.x, e.y, rand.nextDouble() * 4 - 2, rand.nextDouble() * 4 - 2, 15, Color.YELLOW));
                    break;
                }
            }
        }
        bullets.removeIf(b -> b.life <= 0);

        for (Particle p : particles) { p.x += p.vx; p.y += p.vy; p.life--; }
        particles.removeIf(p -> p.life <= 0);

        for (Cloud c : clouds) {
            c.x += c.speed;
            if (c.x > 5000) c.x = -5000;
        }

        camX = player.x - W / 2.0;
        camY = player.y - H / 2.0;

        if (player.hp <= 0) {
            player.hp = player.maxHp;
            player.x = 0; player.y = 0;
            player.vx = 0; player.vy = 0;
            log("飞艇损毁！在起始点重生。", Color.YELLOW);
        }

        if (posLabel != null) {
            posLabel.setText(String.format("坐标: %.0f, %.0f", player.x, player.y));
            spdLabel.setText(String.format("速度: %.0f", Math.hypot(player.vx, player.vy) * 100));
            cargoLabel.setText(String.format("货仓: %d/%d", player.cargo, player.maxCargo));
            hpLabel.setText(String.format("船体: %.0f/%.0f", player.hp, player.maxHp));
            atkLabel.setText("战斗力: " + player.atk);
            woodLabel.setText("木材: " + player.wood);
            oreLabel.setText("矿石: " + player.ore);
            techLabel.setText("科技碎片: " + player.tech);
            energyLabel.setText("能源: " + (int) player.energy);
        }
    }

    private void fireBullet() {
        if (player.energy < 5) return;
        bullets.add(new Bullet(player.x + Math.cos(player.angle) * 25,
            player.y + Math.sin(player.angle) * 25,
            player.angle + rand.nextDouble() * 0.2 - 0.1, 60));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double cx = camX, cy = camY;

        // Grid
        g2d.setColor(new Color(100, 160, 255, 15));
        int gridSize = 200;
        int offX = (int) (-cx % gridSize);
        int offY = (int) (-cy % gridSize);
        for (int x = offX; x < W; x += gridSize) { g2d.drawLine(x, 0, x, H); }
        for (int y = offY; y < H; y += gridSize) { g2d.drawLine(0, y, W, y); }

        // Clouds
        for (Cloud c : clouds) {
            int sx = (int) (c.x - cx), sy = (int) (c.y - cy);
            if (sx < -c.r || sx > W + c.r || sy < -c.r || sy > H + c.r) continue;
            g2d.setColor(new Color(200, 220, 255, (int) (c.opacity * 255)));
            g2d.fillOval(sx - (int) c.r, sy - (int) c.r, (int) c.r * 2, (int) c.r * 2);
        }

        // Islands
        java.util.Map<String, Color> colors = new HashMap<>();
        colors.put("forest", new Color(45, 138, 78));
        colors.put("mine", new Color(110, 110, 122));
        colors.put("ruin", new Color(138, 110, 45));
        colors.put("abandoned", new Color(90, 122, 138));

        for (Island island : islands) {
            int sx = (int) (island.x - cx), sy = (int) (island.y - cy);
            if (sx < -island.r || sx > W + island.r || sy < -island.r || sy > H + island.r) continue;

            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.fillOval(sx - (int) island.r + 8, sy - (int) island.r / 2 + 12, (int) island.r * 2, (int) island.r);

            g2d.setColor(colors.get(island.type));
            g2d.fillOval(sx - (int) island.r, sy - (int) island.r, (int) island.r * 2, (int) island.r * 2);
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.fillOval(sx - (int) (island.r * 0.5), sy - (int) (island.r * 0.5), (int) (island.r * 0.6), (int) (island.r * 0.6));

            g2d.setColor(new Color(204, 232, 255));
            g2d.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
            g2d.drawString(island.name, sx - g2d.getFontMetrics().stringWidth(island.name) / 2, sy + (int) island.r + 16);

            if (island.resources > 0) {
                int barW = (int) (island.r * 1.2);
                double pct = island.resources / 80.0;
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.fillRect(sx - barW / 2, sy + (int) island.r + 22, barW, 4);
                g2d.setColor(pct > 0.5 ? new Color(102, 255, 153) : new Color(255, 204, 102));
                g2d.fillRect(sx - barW / 2, sy + (int) island.r + 22, (int) (barW * pct), 4);
            }

            if (island == nearbyIsland) {
                g2d.setColor(new Color(100, 200, 255, 150));
                g2d.drawOval(sx - (int) island.r - 8, sy - (int) island.r - 8, (int) island.r * 2 + 16, (int) island.r * 2 + 16);
                g2d.drawString("按 E 采集", sx - 30, sy - (int) island.r - 12);
            }
        }

        // Outposts
        for (Outpost o : outposts) {
            int sx = (int) (o.x - cx), sy = (int) (o.y - cy);
            g2d.setColor(new Color(74, 144, 217));
            int[] xs = {sx, sx + 12, sx - 12};
            int[] ys = {sy - 15, sy + 10, sy + 10};
            g2d.fillPolygon(xs, ys, 3);
            g2d.setColor(new Color(136, 204, 255));
            g2d.drawPolygon(xs, ys, 3);
        }

        // Enemies
        for (Enemy e : enemies) {
            int sx = (int) (e.x - cx), sy = (int) (e.y - cy);
            g2d.setColor(e.state.equals("chase") ? new Color(255, 68, 68) : new Color(204, 102, 102));
            int[] xs = {sx + (int) (Math.cos(e.angle) * 12), sx + (int) (Math.cos(e.angle + 2.5) * 8), sx + (int) (Math.cos(e.angle - 2.5) * 8)};
            int[] ys = {sy + (int) (Math.sin(e.angle) * 12), sy + (int) (Math.sin(e.angle + 2.5) * 8), sy + (int) (Math.sin(e.angle - 2.5) * 8)};
            g2d.fillPolygon(xs, ys, 3);
            g2d.setColor(new Color(0, 0, 0, 128));
            g2d.fillRect(sx - 12, sy - 16, 24, 3);
            g2d.setColor(new Color(255, 102, 102));
            g2d.fillRect(sx - 12, sy - 16, (int) (24 * (e.hp / (double) e.maxHp)), 3);
        }

        // Bullets
        g2d.setColor(new Color(136, 221, 255));
        for (Bullet b : bullets) {
            int sx = (int) (b.x - cx), sy = (int) (b.y - cy);
            g2d.fillOval(sx - 3, sy - 3, 6, 6);
        }

        // Player
        int px = W / 2, py = H / 2;
        g2d.translate(px, py);
        g2d.rotate(player.angle);
        g2d.setColor(new Color(74, 144, 217));
        g2d.fillOval(-28, -14, 56, 28);
        g2d.setColor(new Color(136, 204, 255));
        g2d.drawOval(-28, -14, 56, 28);
        g2d.setColor(new Color(110, 200, 255));
        g2d.fillOval(-18, -20, 36, 20);
        g2d.setColor(new Color(160, 216, 255));
        g2d.fillRect(-28, -3, 6, 6);
        g2d.setColor(new Color(204, 232, 255));
        g2d.fillOval(3, -3, 10, 10);
        g2d.rotate(-player.angle);
        g2d.translate(-px, -py);

        if (Math.hypot(player.vx, player.vy) > 0.5) {
            g2d.setColor(new Color(100, 200, 255, 150));
            g2d.fillOval(px - (int) (Math.cos(player.angle) * 32) - 5, py - (int) (Math.sin(player.angle) * 32) - 5, 10, 10);
        }

        // Particles
        for (Particle p : particles) {
            int sx = (int) (p.x - cx), sy = (int) (p.y - cy);
            g2d.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), Math.max(0, (int) (p.life / 30.0 * 255))));
            g2d.fillOval(sx - 2, sy - 2, 4, 4);
        }

        // Weather
        if (weather.equals("storm")) {
            g2d.setColor(new Color(10, 20, 50, 50 + rand.nextInt(30)));
            g2d.fillRect(0, 0, W, H);
            if (rand.nextDouble() < 0.05) {
                g2d.setColor(new Color(200, 220, 255, 40));
                g2d.fillRect(0, 0, W, H);
            }
        }
        if (weather.equals("fog")) {
            g2d.setColor(new Color(200, 220, 240, 50));
            g2d.fillRect(0, 0, W, H);
        }

        // Minimap
        int mmSize = 120;
        int mmX = W - mmSize - 15;
        int mmY = H - mmSize - 15;
        g2d.setColor(new Color(10, 20, 40, 200));
        g2d.fillRect(mmX, mmY, mmSize, mmSize);
        g2d.setColor(new Color(74, 144, 217));
        g2d.drawRect(mmX, mmY, mmSize, mmSize);
        double mmScale = mmSize / 6000.0;
        for (Island island : islands) {
            int ix = mmX + (int) ((island.x + 3000) * mmScale);
            int iy = mmY + (int) ((island.y + 3000) * mmScale);
            g2d.setColor(island.resources > 0 ? new Color(74, 144, 217) : new Color(58, 74, 94));
            g2d.fillOval(ix - 2, iy - 2, 4, 4);
        }
        g2d.setColor(new Color(255, 68, 68));
        for (Enemy e : enemies) {
            int ix = mmX + (int) ((e.x + 3000) * mmScale);
            int iy = mmY + (int) ((e.y + 3000) * mmScale);
            g2d.fillRect(ix - 1, iy - 1, 3, 3);
        }
        g2d.setColor(new Color(102, 255, 153));
        int pxmm = mmX + (int) ((player.x + 3000) * mmScale);
        int pymm = mmY + (int) ((player.y + 3000) * mmScale);
        g2d.fillRect(pxmm - 2, pymm - 2, 5, 5);
    }
}

class Player {
    double x = 0, y = 0, vx = 0, vy = 0, angle = 0;
    double hp = 100, maxHp = 100, speed = 3, energy = 100;
    int cargo = 0, maxCargo = 50, atk = 10;
    int wood = 0, ore = 0, tech = 0;
}

class Island {
    double x, y, r;
    String type, name;
    int resources;
    Island(double x, double y, double r, String type, int resources, String name) {
        this.x = x; this.y = y; this.r = r; this.type = type; this.resources = resources; this.name = name;
    }
    double distanceTo(Island other) {
        return Math.hypot(x - other.x, y - other.y);
    }
}

class Enemy {
    double x, y, angle, speed;
    int hp, maxHp, atk;
    String state = "patrol";
    Enemy(double x, double y, int hp, int atk, double angle, double speed) {
        this.x = x; this.y = y; this.hp = hp; this.maxHp = hp; this.atk = atk;
        this.angle = angle; this.speed = speed;
    }
}

class Bullet {
    double x, y, angle;
    int life;
    Bullet(double x, double y, double angle, int life) {
        this.x = x; this.y = y; this.angle = angle; this.life = life;
    }
}

class Particle {
    double x, y, vx, vy;
    int life;
    Color color;
    Particle(double x, double y, double vx, double vy, int life, Color color) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = life; this.color = color;
    }
}

class Cloud {
    double x, y, r, speed, opacity;
    Cloud(double x, double y, double r, double speed, double opacity) {
        this.x = x; this.y = y; this.r = r; this.speed = speed; this.opacity = opacity;
    }
}

class Outpost {
    double x, y;
    Outpost(double x, double y) { this.x = x; this.y = y; }
}
