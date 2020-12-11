import pandas as pd
from os.path import dirname, join
import numpy as np
from sklearn import svm

# importing training data
filename = join(dirname(__file__), "TrainingV5.csv")
dataset = pd.read_csv(filename)
x = dataset.iloc[:, 0:32]
y = dataset.iloc[:, 32]

# Initialize SVM classifier and train it
clf = svm.SVC(kernel='linear', C=5, probability=True)
clf = clf.fit(x, y)


def callSVM(inputData):
    k2 = []
    for kv in inputData.split(","):
        k2.append(kv)
    predictions = clf.predict([k2])
    probability = clf.predict_proba([k2])
    bumby_Probability = ("%.2f" % round(probability[0][0]*100, 2))
    smooth_Probability = ("%.2f" % round(probability[0][1]*100, 2))
    if(predictions == 1):
        return "Smooth@"+str(smooth_Probability)
    else:
        return "Bumpy@"+str(bumby_Probability)
