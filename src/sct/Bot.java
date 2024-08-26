package sct;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;
import java.util.ListIterator;

public class Bot{
	ArrayList<Bot> objects;
	Random rand = new Random();
	private int x;
	private int y;
	public int xpos;
	public int ypos;
	public Color color;
	public int energy;
	public int minerals;
	public int killed = 0;
	public int[][] map;
	public int[] layers_length = {7, 7, 7, 7, 7, 7};
	public double[][][] weights = new double[layers_length.length][0][0];
	public double[][] neyrons = new double[layers_length.length][0];
	public int[][] neyrons_types = new int[layers_length.length][0];
	private int[] neyrons_types_chance = {50, 49, 1};
	public String[] neyrons_types_names = {"RELU", "sigmoid", "random"};
	public int age = 1000;
	public int state = 0;//бот или органика
	public int state2 = 1;//что ставить в массив с миром
	private int rotate = rand.nextInt(8);
	private int[][] movelist = {
		{0, -1},
		{1, -1},
		{1, 0},
		{1, 1},
		{0, 1},
		{-1, 1},
		{-1, 0},
		{-1, -1}
	};
	private int[] minerals_list = {1, 2, 3};
	private int[] photo_list = {10, 8, 6, 5, 4, 3};
	private int[] world_scale = {162, 108};
	private int c_red = 0;
	private int c_green = 0;
	private int c_blue = 0;
	private int sector_len = world_scale[1] / 8;
	private int result;
	private int command;
	public Bot(int new_xpos, int new_ypos, Color new_color, int new_energy, int[][] new_map, ArrayList<Bot> new_objects) {
		xpos = new_xpos;
		ypos = new_ypos;
		x = new_xpos * 10;
		y = new_ypos * 10;
		color = new_color;
		energy = new_energy;
		minerals = 0;
		objects = new_objects;
		map = new_map;
		for (int i = 0; i < layers_length.length; i++) {
			neyrons[i] = get_layer(layers_length[i]);
			neyrons_types[i] = new int[layers_length[i]];
			if (i < layers_length.length - 1) {
				weights[i] = get_weights(layers_length[i], layers_length[i + 1]);
				for (int j = 0; j < layers_length[i]; j++) {
					neyrons_types[i][j] = get_neyron_type();
					for (int k = 0; k < layers_length[i + 1]; k++) {
						weights[i][j][k] = random();
					}
				}
			}
		}
		
	}
	public void Draw(Graphics canvas, int draw_type) {
		if (state == 0) {//рисуем бота
			canvas.setColor(new Color(0, 0, 0));
			canvas.fillRect(x, y, 10, 10);
			if (draw_type == 0) {//режим отрисовки хищников
				int r = 0;
				int g = 0;
				int b = 0;
				if (c_red + c_green + c_blue == 0) {
					r = 128;
					g = 128;
					b = 128;
				}else {
					r = (int)((c_red * 1.0) / (c_red + c_green + c_blue) * 255.0);
					g = (int)((c_green * 1.0) / (c_red + c_green + c_blue) * 255.0);
					b = (int)((c_blue * 1.0) / (c_red + c_green + c_blue) * 255.0);
				}
				canvas.setColor(new Color(r, g, b));
			}else if (draw_type == 1) {//цвета
				canvas.setColor(color);
			}else if (draw_type == 2) {//энергии
				int g = 255 - (int)(energy / 1000.0 * 255.0);
				if (g > 255) {
					g = 255;
				}else if (g < 0) {
					g = 0;
				}
				canvas.setColor(new Color(255, g, 0));
			}else if (draw_type == 3) {//минералов
				int rg = 255 - (int)(minerals / 1000.0 * 255.0);
				if (rg > 255) {
					rg = 255;
				}else if (rg < 0) {
					rg = 0;
				}
				canvas.setColor(new Color(rg, rg, 255));
			}else if (draw_type == 4) {//возраста
				canvas.setColor(new Color((int)(age / 1000.0 * 255.0), (int)(age / 1000.0 * 255.0), (int)(age / 1000.0 * 255.0)));
			}
			canvas.fillRect(x + 1, y + 1, 8, 8);
		}else {//рисуем органику
			canvas.setColor(new Color(0, 0, 0));
			canvas.fillRect(x + 1, y + 1, 8, 8);
			canvas.setColor(new Color(128, 128, 128));
			canvas.fillRect(x + 2, y + 2, 6, 6);
		}
	}
	public int Update(ListIterator<Bot> iterator) {
		if (killed == 0) {
			if (state == 0) {//бот
				int sector = bot_in_sector();
				energy -= 1;
				age -= 1;
				if (sector <= 7 & sector >= 5) {
					minerals += minerals_list[sector - 5];
				}
				update_commands(iterator);
				if (energy <= 0) {
					killed = 1;
					map[xpos][ypos] = 0;
					return(0);
				}else if (energy > 1000) {
					energy = 1000;
				}
				if (age <= 0) {
					state = 1;
					state2 = 2;
					map[xpos][ypos] = 2;
					return(0);
				}
				if (minerals > 1000) {
					minerals = 1000;
				}
			}else if (state == 1) {//падающая органика
				move(4);
				int[] pos = get_rotate_position(4);
				if (pos[1] > 0 & pos[1] < world_scale[1]) {
					if (map[pos[0]][pos[1]] != 0) {
						state = 2;
					}
				}
			}else {//стоящая органика
				//
			}
		}
		return(0);
	}
	public void update_commands(ListIterator<Bot> iterator) {//мозг
		for (int weightx = 0; weightx < layers_length.length; weightx++) {
			for (int weighty = 0; weighty < layers_length[weightx]; weighty++) {
				if (weightx == 0) {//входы
					if (weighty == 0) {
						neyrons[0][0] = xpos / 162.0;
					}else if (weighty == 1) {
						neyrons[0][1] = ypos / 108.0;
					}else if (weighty == 2) {
						neyrons[0][2] = energy / 1000.0;
					}else if (weighty == 3) {
						neyrons[0][3] = minerals / 1000.0;
					}else if (weighty == 4) {
						neyrons[0][4] = rotate / 8.0;
					}else if (weighty == 5) {
						neyrons[0][5] = age / 1000.0;
					}else if (weighty == 6) {
						result = 0;
						int[] pos = get_rotate_position(rotate);
						if (pos[1] > 0 & pos[1] < world_scale[1]) {
							if (map[pos[0]][pos[1]] == 0) {
								result = 0;//если ничего
							}else if (map[pos[0]][pos[1]] == 1) {
								Bot b = find(pos);
								if (b != null) {
									if (is_relative(weights, b.weights)) {
										result = 3;//если родственник
									}else {
										result = 2;//если враг
									}
								}else {
									result = 0;//если ничего
								}
							}else if (map[pos[0]][pos[1]] == 2) {
								result = 4;//если органика
							}
						}else {
							result = 1;//если граница
						}
						neyrons[0][6] = result / 4.0;
					}
				}
				if (weightx < layers_length.length - 1) {//проход по весам
					double data = 0;
					if (weightx > 0) {//функция активации
						String func = neyrons_types_names[neyrons_types[weightx][weighty]];
						if (func == "RELU") {
							data = RELU(neyrons[weightx][weighty]);//простая
						}else if (func == "sigmoid") {
							data = sigmoid(neyrons[weightx][weighty]);//логистическая
						}else if (func == "random") {
							data = random_neyron(neyrons[weightx][weighty]);//случайная
						}
					}else {
						data = neyrons[weightx][weighty];
					}
					neyrons[weightx][weighty] = 0;//сбросить значение
					for (int weight = 0; weight < layers_length[weightx + 1]; weight++) {//умножение весов
						neyrons[weightx + 1][weight] += data * weights[weightx][weighty][weight];
					}
				}
			}
		}
		if (neyrons[layers_length.length - 1][5] > 0) {
			if (neyrons[layers_length.length - 1][6] > 0) {
				rotate += 1;
				if (rotate > 7) {
					rotate = 0;
				}
			}else {
				rotate -= 1;
				if (rotate < 0) {
					rotate = 7;
				}
			}
		}
		double max = -99999.0;
		for (int i = 0; i < 7; i++) {
			if (neyrons[layers_length.length - 1][i] > max && i < 5) {
				command = i;
				max = neyrons[layers_length.length - 1][i];
				neyrons[layers_length.length - 1][i] = 0;
			}
		}
		if (command == 0) {
			move(rotate);
			energy -= 1;
		}else if (command == 1) {
			attack(rotate);
		}else if (command == 2) {
			multiply(rotate, iterator);
		}else if (command == 3) {
			int sector = bot_in_sector();
			if (sector <= 5) {
				energy += photo_list[sector];
				c_green += 1;
			}
		}else if (command == 4) {
			if (minerals > 0) {
				c_blue++;
			}
			energy += minerals * 4;
			minerals = 0;
		}
	}
	public void give(int rot) {
		int[] pos = get_rotate_position(rot);
		if (pos[1] > 0 & pos[1] < world_scale[1]) {
			if (map[pos[0]][pos[1]] == 1) {
				Bot relative = find(pos);
				if (relative.killed == 0) {
					relative.energy += energy / 4;
					relative.minerals += minerals / 4;
					energy -= energy / 4;
					minerals -= minerals / 4;
				}
			}
		}
	}
	public void give2(int rot) {
		int[] pos = get_rotate_position(rot);
		if (pos[1] > 0 & pos[1] < world_scale[1]) {
			if (map[pos[0]][pos[1]] == 1) {
				Bot relative = find(pos);
				if (relative.killed == 0) {
					int enr = relative.energy + energy;
					int mnr = relative.minerals + minerals;
					relative.energy = enr / 2;
					relative.minerals = mnr / 2;
					energy = enr / 2;
					minerals = mnr / 2;
				}
			}
		}
	}
	public void attack(int rot) {
		int[] pos = get_rotate_position(rot);
		if (pos[1] > 0 & pos[1] < world_scale[1]) {
			if (map[pos[0]][pos[1]] != 0) {
				Bot victim = find(pos);
				if (victim != null) {
					victim.killed = 1;
					energy += victim.energy;
					map[pos[0]][pos[1]] = 0;
					c_red++;
				}
			}
		}
	}
	public Bot find(int[] pos) {//только если есть сосед
		for (Bot b: objects) {
			if (b.killed == 0 & b.xpos == pos[0] & b.ypos == pos[1]) {
				return(b);
			}
		}
		return(null);
	}
	public boolean is_relative(double[][][] brain1, double[][][] brain2) {
		int errors = 0;
		for (int i = 0; i < layers_length.length; i++) {
			for (int j = 0; j < layers_length[i]; j++) {
				if (i < layers_length.length - 1) {
					for (int k = 0; k < layers_length[i + 1]; k++) {
						if (brain1[i][j][k] != brain2[i][j][k]) {
							errors++;
						}
						if (errors > 9) {
							return(false);
						}
					}
				}
			}
		}
		return(errors < 8);
	}
	public int[] get_rotate_position(int rot){
		int[] pos = new int[2];
		pos[0] = (xpos + movelist[rot][0]) % world_scale[0];
		pos[1] = ypos + movelist[rot][1];
		if (pos[0] < 0) {
			pos[0] = 161;
		}else if(pos[0] >= world_scale[0]) {
			pos[0] = 0;
		}
		return(pos);
	}
	public int move(int rot) {
		int[] pos = get_rotate_position(rot);
		if (pos[1] > 0 & pos[1] < world_scale[1]) {
			if (map[pos[0]][pos[1]] == 0) {
				map[xpos][ypos] = 0;
				xpos = pos[0];
				ypos = pos[1];
				x = xpos * 10;
				y = ypos * 10;
				map[xpos][ypos] = state2;
				return(1);
			}
		}
		return(0);
	}
	public void multiply(int rot, ListIterator<Bot> iterator) {
		int[] pos = get_rotate_position(rot);
		if (pos[1] > 0 & pos[1] < world_scale[1]) {
			if (map[pos[0]][pos[1]] == 0) {
				energy -= 150;
				if (energy <= 0) {
					killed = 1;
					map[xpos][ypos] = 0;
				}else {
					map[pos[0]][pos[1]] = 1; 
					Color new_color = new Color(color.getRed(), color.getGreen(), color.getBlue());
					double[][][] new_brain = new double[layers_length.length][0][0];
					int[][] new_neyrons_types = new int[layers_length.length][0];
					for (int i = 0; i < layers_length.length; i++) {
						new_neyrons_types[i] = new int[layers_length[i]];
						if (i < layers_length.length - 1) {
							new_brain[i] = get_weights(layers_length[i], layers_length[i + 1]);
							for (int j = 0; j < layers_length[i]; j++) {
								new_neyrons_types[i][j] = neyrons_types[i][j];
								for (int k = 0; k < layers_length[i + 1]; k++) {
									//if (rand.nextInt(50) == 0) {//мутация
									//	new_brain[i][j][k] = random();
									//}else {
									//	new_brain[i][j][k] = weights[i][j][k];
									//}
									new_brain[i][j][k] = weights[i][j][k];
								}
							}
						}
					}
					if (rand.nextInt(4) == 0) {//мутация
						new_color = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
						for (int i = 0; i < 8; i++) {
							int dri = rand.nextInt(layers_length.length - 1);
							int drj = rand.nextInt(layers_length[dri]);
							int drk = rand.nextInt(layers_length[dri + 1]);
							new_brain[dri][drj][drk] = random();
						}
						for (int i = 0; i < 4; i++) {
							int dri = rand.nextInt(layers_length.length - 1);
							int drj = rand.nextInt(layers_length[dri]);
							new_neyrons_types[dri][drj] = get_neyron_type();
						}
					}
					//if (rand.nextInt(400) == 0) {
					//	new_color = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
					//}else {
					//	new_color = new Color(border(new_color.getRed() - 12 + rand.nextInt(25), 255), border(new_color.getGreen() - 12 + rand.nextInt(25), 255), border(new_color.getBlue() - 12 + rand.nextInt(25), 255));
					//}
					Bot new_bot = new Bot(pos[0], pos[1], new_color, energy / 2, map, objects);
					new_bot.minerals = minerals / 2;
					energy /= 2;
					minerals /= 2;
					new_bot.weights = new_brain;
					new_bot.neyrons_types = new_neyrons_types;
					iterator.add(new_bot);
				}
			}
		}
	}
	public int bot_in_sector() {
		int sec = ypos / sector_len;
		if (sec > 7) {
			sec = 10;
		}
		return(sec);
	}
	public double[] get_layer(int len) {
		return(new double[len]);
	}
	public double[][] get_weights(int len, int len2) {
		return(new double[len][len2]);
	}
	public int get_neyron_type() {
		int chance = rand.nextInt(100);
		int sum = 0;
		for (int i = 0; i < neyrons_types_chance.length; i++) {
			if (chance < sum + neyrons_types_chance[i]) {
				return(i);
			}
			sum += neyrons_types_chance[i];
		}
		return(0);
	}
	public int border(int number, int border) {
		if (number > border) {
			number = border;
		}else if (number < 0) {
			number = 0;
		}
		return(number);
	}
	public double RELU(double f) {
		if (f > 0) {
			return(f);
		}else {
			return(f * 0.01);
		}
	}
	public double sigmoid(double f) {
		return(1 / (1 + Math.pow(2.718, -f)));
	}
	public double radial_basis(double f) {
		if (f >= 0.45 && f <= 0.55) {
			return(1.0);
		}else {
			return(0.0);
		}
	}
	public double simple(double f) {
		if (f > 0.5) {
			return(1.0);
		}else {
			return(0.0);
		}
	}
	public double random_neyron(double f) {
		if (f > Math.random()) {
			return(1.0);
		}else {
			return(0.0);
		}
	}
	public double th(double f) {
		return((Math.pow(2.718, f) - Math.pow(2.718, -f)) / (Math.pow(2.718, f) + Math.pow(2.718, -f)));
	}
	public double sin(double f) {
		return(Math.sin(f));
	}
	public double random() {
		if (rand.nextInt(2) == 1){
			return(Math.random());
		}else {
			return(Math.random() * -1);
		}
	}
}
