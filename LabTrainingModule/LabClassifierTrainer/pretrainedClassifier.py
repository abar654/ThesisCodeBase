from sklearn import svm
from sklearn import neighbors
from sklearn import cross_validation
from sklearn import metrics
from sklearn import preprocessing
from sklearn.externals import joblib
from mlxtend.feature_selection import SequentialFeatureSelector as SFS

import numpy as np
import math
import csv

import warnings
warnings.filterwarnings("ignore")

#A file that tests the pretraining classifier
#Partitions the data into training and validation sets
#Tunes hyper parameters on the training set
#Trains a classifier with the found params on training set
#Verifies the score that this classifier acheives on validation set

cv_folds = 5

fivePointScale = True

if fivePointScale :
    ratingsFile = "pretrainerRatings.csv"
else :
    ratingsFile = "pretrainerRatings3.csv"

#Open the csv file of physiological features output by matlab
file = open("pretrainerFeatures.csv", "r")
physData = preprocessing.scale(np.loadtxt(file, delimiter=","))

#Load the target ratings
file = open(ratingsFile, "r")
physTarget = np.loadtxt(file, delimiter=",")

classifier = svm.SVC(gamma=1, C=1)
    
#Fit the classifier to the training data
classifier.fit(physData, physTarget)

#Save the classifier to be opened in another file
joblib.dump(classifier, 'pretrainedClf.pkl')
