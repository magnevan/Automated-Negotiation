import numpy as np
from matplotlib import pyplot as plt

t = np.linspace(0, 1, 100)
s = 0.35
r = 0.25


plt.axes(xlim=(0.0,1.0), ylim=(0.0,1.1))
plt.xlabel('Time')
plt.ylabel('Utility')
plt.plot(t, (1-r)*(1 - t**s) + r, 'b', [0, 1], [r, r], 'g--')
plt.show()
