width = 60
height = 60

import matplotlib.pyplot as plt
from matplotlib import colors
import numpy as np


dx0, dx1, dx2, dx3, dx4, dx5, dx6 = width//7, width//7, width//7, width//7, width//7, width//7, width//7
dy0, dy1, dy2, dy3, dy4, dy5, dy6 = height//7, height//7, height//7, height//7, height//7, height//7, height//7


if width % 7 == 1:
    dx6 += 1

elif width % 7 == 2:
    dx6 += 1
    dx0 += 1

elif width % 7 == 3:
    dx0 += 1
    dx3 += 1
    dx6 += 1

elif width % 7 == 4:
    dx0 += 1
    dx3 += 1
    dx4 += 1
    dx6 += 1

elif width % 7 == 5:
    dx1 += 1
    dx2 += 1
    dx3 += 1
    dx4 += 1
    dx5 += 1

elif width % 7 == 6:
    dx1 += 1
    dx2 += 1
    dx3 += 1
    dx4 += 1
    dx5 += 1
    dx6 += 1

##################################################################################################
##################################################################################################
##################################################################################################

if height % 7 == 1:
    dy6 += 1

elif height % 7 == 2:
    dy6 += 1
    dy0 += 1

elif height % 7 == 3:
    dy0 += 1
    dy3 += 1
    dy6 += 1

elif height % 7 == 4:
    dy0 += 1
    dy3 += 1
    dy4 += 1
    dy6 += 1

elif height % 7 == 5:
    dy1 += 1
    dy2 += 1
    dy3 += 1
    dy4 += 1
    dy5 += 1

elif height % 7 == 6:
    dy1 += 1
    dy2 += 1
    dy3 += 1
    dy4 += 1
    dy5 += 1
    dy6 += 1


x_incr = [dx0, dx1, dx2, dx3, dx4, dx5, dx6]
y_incr = [dy0, dy1, dy2, dy3, dy4, dy5, dy6]


def gen_row():
    row = []
    index = 0
    for x_delta in x_incr:
        for _ in range(x_delta):
            row.append(index)
        index += 1
    return row


grid = []
y_index = 0
for y_delta in y_incr:
    for _ in range(y_delta):
        row = gen_row()
        for i in range(len(row)):
            row[i] += y_index * 7
        grid.append(row)
    y_index += 1


x_major_tick = 0
x_ticks = []
for x_tick in x_incr:
    x_major_tick += x_tick
    x_ticks.append(x_major_tick)

y_major_tick = 0
y_ticks = []
for y_tick in y_incr:
    y_major_tick += y_tick
    y_ticks.append(y_major_tick)


ax = plt.gca()
ax.set_xticks(np.arange(width)-0.5, labels=[str(i) for i in range(width)])
ax.set_yticks(np.arange(height)-0.5, labels=[str(i) for i in range(height)])


plt.xlim(0,width)
plt.ylim(0,height)

ax.spines[:].set_visible(False)

# ax.set_xticks(np.arange(width)-.5, minor=True)
# ax.set_yticks(np.arange(height)-.5, minor=True)

ax.grid(which='major', color='w', linestyle='-', linewidth=0.5)
ax.tick_params(which="major", bottom=False, left=False)


im = ax.imshow(grid, interpolation='none', aspect='auto', origin='lower')


# setting labels
data = im.get_array()
for i in range(data.shape[0]):
    for j in range(data.shape[1]):
        text = im.axes.text(i, j, str((i, j)),fontsize=3, color = 'white')


plt.show()