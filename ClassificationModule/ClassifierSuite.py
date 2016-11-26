from sklearn import svm
from sklearn import neighbors
from sklearn import decomposition
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

#A file that tests all the classifiers.
#Partitions the data into training and validation sets
#Tunes hyper parameters on the training set
#Trains a classifier with the found params on training set
#Verifies the score that this classifier acheives on validation set

cv_folds = 5

#Loop parameters
dataTypes = ['Reviews', 'Ratings', 'Combined']
featureTypes = ['phone', 'physiological', 'both', 'physPlus', 'bothPlus']
fivePointScaleOptions = [True, False]

num_vals = [15]
k_vals = [5, 7, 10, 20]
c_vals = [100, 10]
gamma_vals = [1, 0.1, 0.01]
balanced_vals = [False]
linear_vals = [True, False] #Linear was consistently worse and it slows down the program hence removed.
scoreMethods = ['accuracy', 'mean_absolute_error', 'mean_squared_error']

#Settings
featureSelectEnabled = False
postFeatSelEnabled = False
pcaEnabled = True
floatingEnabled = True
printHyperParamTuning = False
printSelectedFeats = True
dfs = 'ovo' #Decision function shape, default ovo seems best
showSVM = True
showKNN = True
displayAcc = True
displayMSE = True
displayMAE = True
displayDistributions = False

output = open("results.txt", "a")
print('FeatureSelEnabled: ' + str(featureSelectEnabled) + ', PCAEnabled: ' + str(pcaEnabled) + ', CV Folds: ' + str(cv_folds))
output.write('FeatureSelEnabled: ' + str(featureSelectEnabled) + ', PCAEnabled: ' + str(pcaEnabled) + ', CV Folds: ' + str(cv_folds) + '\n')
output.close()

resultMatrix = np.zeros((len(featureTypes)*len(dataTypes)*len(fivePointScaleOptions)*2, 36))

resultRowIndex = 0;

for fivePointScale in fivePointScaleOptions :
    for dataType in dataTypes:

        if fivePointScale :
            scale = "FivePointScale"
        else :
            scale = "ThreePointScale"

        #Open the csv file of smart phone features output by the java program
        file = open(scale+"\\"+dataType+"\\"+"emotionFeatures.csv", "r")
        dataset = np.loadtxt(file, delimiter=",")

        #Separate the smartphone data and the target labels
        phoneData = dataset[:, :-1]
        emotion_target = dataset[:, -1]

        #Load the phoneHeader
        phFile = open("emotionHeader.csv", "r")
        phInput = phFile.readline()
        phoneHeader = phInput.strip().split(',')
        phoneHeader = phoneHeader[:-1]

        #Open the physiological feature file
        physFile = open(scale+"\\"+dataType+"\\"+"physFeatures.csv", "r")
        physData = np.loadtxt(physFile, delimiter=",")

        #Load the physHeader
        phFile = open("physHeader.csv", "r")
        phInput = phFile.readline()
        physHeader = phInput.strip().split(',')

        #Find the rows which are not all -1 for physiological features
        voidRowSum = len(physData[0]) * -1
        numRows = len(physData)
        validRows = []

        for i in range(0, numRows):
            if(sum(physData[i]) != voidRowSum):
                validRows.append(i)

        #Load the pretrainer data
        preClassifier = joblib.load('pretrainedClf.pkl')

        #For everyline in the physData matrix we want to run the preclassifier
        #on the appropriate features
        #Note: Order TEMP,EDA,BVP,HR each with 27 features = 108 features
        #       and this is the same order as in physData therefore first 108
        pretrainData = physData[: , 0:108]
        pretrainPrediction = np.transpose([preClassifier.predict(pretrainData)])

        #This only has the first, most basic addition - Still need to add windowing
        physPlusData = np.concatenate((physData,pretrainPrediction),axis=1)

        #Read in the timestamps so you can know which rows to attribute each
        #window to
        file = open(scale+"\\"+dataType+"\\"+"times.csv", "r")
        timestamps = np.loadtxt(file, delimiter=",")

        #Read in the windowed data one line at a time and save data in a dict
        file = open(scale+"\\"+dataType+"\\"+"windowedFeatures.csv", "r")

        currLabelTime = 0;
        currRatingsHist = [0, 0, 0, 0, 0]
        ratingHists = {}
        
        for line in file:

            #Decompose line
            fields = line.strip().split(',')

            #First element will be the timestamp label
            windowTime = fields[0]

            #Fields 1 to 108 should be the data
            windowData = fields[1:109]

            #Classify the window
            windowRating = int(preClassifier.predict(windowData)[0])

            if currLabelTime != windowTime :

                #Store the current data in the hashmap and update
                if currLabelTime != 0 :
                    ratingHists[currLabelTime] = currRatingsHist

                #Update the curr values
                currLabelTime = windowTime
                currRatingsHist = [0, 0, 0, 0, 0]

            currRatingsHist[windowRating-1] = currRatingsHist[windowRating-1] + 1

        #Now go through the valid time stamps and create features for each
        windowFeatures = np.zeros((len(timestamps), 6))

        for timeIndex in range(0, len(timestamps)) :

            if timestamps[timeIndex] in ratingHists :
                currHist = ratingHists[timestamps[timeIndex]]

                #Find the number of ratings
                numRatings = currHist[0] + currHist[1] + currHist[2] + currHist[3] + currHist[4]

                #Average the results in the curr hist
                avgRating = (currHist[0]*1 + currHist[1]*2 + currHist[2]*3 + currHist[3]*4 + currHist[4]*5)/numRatings

                for rateIndex in range(0, len(currHist)) :
                    windowFeatures[timeIndex, rateIndex] = currHist[rateIndex]/numRatings

                windowFeatures[timeIndex, 5] = avgRating

        #Add windowFeatures to the physPlusData
        physPlusData = np.concatenate((physPlusData,windowFeatures),axis=1)

        for featureType in featureTypes:

            #Setup the data and targets
            if featureType == 'phone' :
        
                currentData = preprocessing.scale(phoneData)
                currentTarget = emotion_target
                currentHeader = phoneHeader
                numPCs = 20

            elif featureType == 'physiological' :

                currentData = preprocessing.scale(physData[validRows, ])
                currentTarget = emotion_target[validRows, ]
                currentHeader = physHeader
                numPCs = 30

            elif featureType == 'both' :

                tempPhysData = preprocessing.scale(physData[validRows, ])
                tempPhoneData = preprocessing.scale(phoneData[validRows, ])

                currentData = np.concatenate((tempPhysData,tempPhoneData),axis=1)
                currentTarget = emotion_target[validRows, ]
                currentHeader = physHeader + phoneHeader
                numPCs = 50

            elif featureType == 'physPlus' :

                currentData = preprocessing.scale(physPlusData[validRows, ])
                currentTarget = emotion_target[validRows, ]
                currentHeader = physHeader + ['physPretrainWhole', 'winHist1',  'winHist2', 'winHist3', 'winHist4', 'winHist5', 'winAvg']
                numPCs = 30

            elif featureType == 'bothPlus' :

                tempPhysData = preprocessing.scale(physPlusData[validRows, ])
                tempPhoneData = preprocessing.scale(phoneData[validRows, ])

                currentData = np.concatenate((tempPhysData,tempPhoneData),axis=1)
                currentTarget = emotion_target[validRows, ]
                currentHeader = physHeader + ['physPretrainWhole', 'winHist1',  'winHist2', 'winHist3', 'winHist4', 'winHist5', 'winAvg'] + phoneHeader
                numPCs = 50

            else :
                print("Feature type error!")

            print(featureType, dataType, "features:", len(currentData[0]), "samples:", len(currentData))

            #Create a training set and validation set
            indices = np.random.permutation(len(currentData))
            numValSamples = math.ceil(len(currentData)/4) #Use 3/4 of data for training

            data_train = currentData[indices[:-numValSamples]]
            target_train = currentTarget[indices[:-numValSamples]]
            data_val = currentData[indices[-numValSamples:]]
            target_val = currentTarget[indices[-numValSamples:]]

            #Calculate the distribution of ratings in the training and val sets
            #Calculate what accuracy the trivial dominant class classifier would get
            output = open("results.txt", "a")
            
            #Train distribution
            trainDist = [0,0,0,0,0]
            for rateValue in target_train :
                trainDist[int(rateValue)-1] += 1

            if displayDistributions:
                print("-------------------------------------------")
                output.write("-------------------------------------------\n")
                print("Training Distribution: ", end="")
                output.write("Training Distribution: ")

                for rateIndex in range(0, len(trainDist)) :
                    print(str(trainDist[rateIndex]/len(target_train)) + " (" + str(trainDist[rateIndex]) + ") ", end="")
                    output.write(str(trainDist[rateIndex]/len(target_train)) + " (" + str(trainDist[rateIndex]) + ") ")
                print()
                output.write("\n")

            #Validation Distribution
            valDist = [0,0,0,0,0]
            for rateValue in target_val :
                valDist[int(rateValue)-1] += 1

            if displayDistributions:
                print("Validation Distribution: ", end="")
                output.write("Validation Distribution: ")

                for rateIndex in range(0, len(valDist)) :
                    print(str(valDist[rateIndex]/len(target_val)) + " (" + str(valDist[rateIndex]) + ") ", end="")
                    output.write(str(valDist[rateIndex]/len(target_val)) + " (" + str(valDist[rateIndex]) + ") ")

                print()
                output.write("\n")

                print("-------------------------------------------")
                output.write("-------------------------------------------\n")

            #Trivial classifier score
            trivialIndex = trainDist.index(max(trainDist))
            trivAcc = valDist[trivialIndex]/len(target_val)
            triv_prediction = np.full((len(target_val),1), trivialIndex+1, dtype=int)
            trivMAE = metrics.mean_absolute_error(target_val, triv_prediction)
            trivMSE = metrics.mean_squared_error(target_val, triv_prediction)

            output.close()

            #Output the data describing the current classifier type
            clfFile = open("clfTypes.csv", "a")
            if showSVM :
                clfFile.write(dataType + ',' + featureType + ',' + scale + ',SVM\n')
            if showKNN :
                clfFile.write(dataType + ',' + featureType + ',' + scale + ',KNN\n')
            clfFile.close()

            resultSVMIndex = 0;
            resultKNNIndex = 0;
            
            for scoreMethod in scoreMethods:
                #Find the best SVM for this data
                if showSVM :
                    #Open a file for output
                    output = open("results.txt", "a")
                    print("----------" + " SVM " + dataType + " " + featureType +  " ----------")
                    output.write("----------" + " SVM " + dataType + " " + featureType +  " ----------\n")
                    
                    c_best = 0
                    gamma_best = 0
                    num_best = 0
                    bal_best = False
                    lin_best = False
                    acc_best = -5

                    for c_curr in c_vals:
                        for gamma_curr in gamma_vals:
                            for numFeatures in num_vals:
                                for balanced in balanced_vals:
                                    for linear in linear_vals:
                                        #Find CV accuracy with current params
                                        #Set up the classifier
                                        if balanced :
                                            curr_class_weight='balanced'
                                        else:
                                            curr_class_weight=None

                                        if linear :
                                            classifier = svm.LinearSVC(C=c_curr, class_weight=curr_class_weight)
                                        else :
                                            classifier = svm.SVC(gamma=gamma_curr, C=c_curr, class_weight=curr_class_weight, decision_function_shape=dfs)

                                        if featureSelectEnabled :    
                                            #Set up the Sequential Feature Selector
                                            #Here we will use Sequential Floating Forward Selection
                                            sffs = SFS(classifier, 
                                                       k_features=numFeatures, 
                                                       forward=True, 
                                                       floating=floatingEnabled, 
                                                       scoring=scoreMethod,
                                                       print_progress=False,
                                                       cv=cv_folds)
                                            sffs = sffs.fit(data_train, target_train)

                                            if printHyperParamTuning :
                                                print('CV Score: ' + str(sffs.k_score_) + ', C: ' + str(c_curr) + ', gamma: ' + str(gamma_curr) + ', numFeatures: ' + str(numFeatures) + ', balanced: ' + str(balanced) + ', lin: ' + str(linear))
                                                output.write('CV Score: ' + str(sffs.k_score_) + ', C: ' + str(c_curr) + ', gamma: ' + str(gamma_curr) + ', numFeatures: ' + str(numFeatures) + ', balanced: ' + str(balanced) + ', lin: ' + str(linear) + '\n')

                                            #Test if better than current best and if yest replace vals
                                            if sffs.k_score_ > acc_best:
                                                acc_best = sffs.k_score_
                                                c_best = c_curr
                                                gamma_best = gamma_curr
                                                num_best = numFeatures
                                                bal_best = balanced
                                                lin_best = linear
                                        else :
                                            #Use cross validation to find Accuracy
                                            emotion_scores = cross_validation.cross_val_score(classifier, data_train, target_train, scoring=scoreMethod, cv=5)

                                            if printHyperParamTuning :
                                                print('CV Score: ' + str(np.mean(emotion_scores)) + ', C: ' + str(c_curr) + ', gamma: ' + str(gamma_curr) + ', balanced: ' + str(balanced) + ', lin: ' + str(linear))
                                                output.write('CV Score: ' + str(np.mean(emotion_scores)) + ', C: ' + str(c_curr) + ', gamma: ' + str(gamma_curr) + ', balanced: ' + str(balanced) + ', lin: ' + str(linear) + '\n')

                                            if np.mean(emotion_scores) > acc_best:
                                                acc_best = np.mean(emotion_scores)
                                                c_best = c_curr
                                                gamma_best = gamma_curr
                                                bal_best = balanced
                                                lin_best = linear

                    #Now that we have the best parameters we can create
                    #a classifier to test our validation set

                    print('C: ', c_best, ', gamma: ', gamma_best, 'linear: ', lin_best)

                    output.write('C: ' + str(c_best) + ', gamma: ' + str(gamma_best) + 'linear: ' + str(lin_best) + "\n")

                    if featureSelectEnabled or postFeatSelEnabled :

                        if printHyperParamTuning :
                            print('Accuracy max: ', acc_best)
                            print('C: ', c_best)
                            print('gamma: ', gamma_best)
                            print('numFeatures: ', num_best)
                            print('balanced: ', bal_best)
                            print('linear: ', lin_best)

                            output.write('Accuracy max: ' + str(acc_best) + "\n")
                            output.write('C: ' + str(c_best) + "\n")
                            output.write('gamma: ' + str(gamma_best) + "\n")
                            output.write('numFeatures: ' + str(num_best) + "\n")
                            output.write('balanced: ' + str(bal_best) + "\n")
                            output.write('linear: ' + str(lin_best) + "\n")
                                            
                        if bal_best :
                            curr_class_weight='balanced'
                        else:
                            curr_class_weight=None

                        if lin_best :
                            classifier = svm.LinearSVC(C=c_curr, class_weight=curr_class_weight)
                        else :
                            classifier = svm.SVC(gamma=gamma_curr, C=c_curr, class_weight=curr_class_weight, decision_function_shape=dfs)          

                        sffs = SFS(classifier, 
                                   k_features=20, 
                                   forward=True, 
                                   floating=floatingEnabled, 
                                   scoring=scoreMethod,
                                   print_progress=False,
                                   cv=cv_folds)

                        #Select the features
                        sffs = sffs.fit(data_train, target_train)
                        data_sffs_train = sffs.transform(data_train)
                        data_sffs_val = sffs.transform(data_val)

                        #Fit the classifier to the training data
                        classifier.fit(data_sffs_train, target_train)

                        #Print the features selected
                        if printSelectedFeats :
                            print('Selected features: ', end="")
                            output.write('Selected features: ')
                            for feat in sffs.k_feature_idx_ :
                                print(currentHeader[feat], end=",")
                                output.write(currentHeader[feat] + ",")
                            print()
                            output.write("\n")

                        #Find the accuracy on the validation data
                        if displayAcc:
                            print('Feat select validation set accuracy:', classifier.score(data_sffs_val, target_val))
                            output.write('Feat select validation set accuracy:' + str(classifier.score(data_sffs_val, target_val)) + "\n")

                            resultMatrix[resultRowIndex, resultSVMIndex] = classifier.score(data_sffs_val, target_val)
                            resultSVMIndex = resultSVMIndex + 1

                        if displayMAE:
                            pred_val = classifier.predict(data_sffs_val)
                            result = metrics.mean_absolute_error(target_val, pred_val)

                            #Find the accuracy on the validation data
                            print('Feat select validation set MAE:', result)
                            output.write('Feat select validation set MAE:' + str(result) + "\n")

                            resultMatrix[resultRowIndex, resultSVMIndex] = result
                            resultSVMIndex = resultSVMIndex + 1

                        if displayMSE:
                            pred_val = classifier.predict(data_sffs_val)
                            result = metrics.mean_squared_error(target_val, pred_val)

                            #Find the accuracy on the validation data
                            print('Feat select validation set MSE:', result)
                            output.write('Feat select validation set MSE:' + str(result) + "\n")
                            resultMatrix[resultRowIndex, resultSVMIndex] = result
                            resultSVMIndex = resultSVMIndex + 1
                                  
                    if printHyperParamTuning :
                        print('Accuracy max: ', acc_best)
                        print('C: ', c_best)
                        print('gamma: ', gamma_best)
                        print('balanced: ', bal_best)
                        print('linear: ', lin_best)

                        output.write('Accuracy max: ' + str(acc_best) + "\n")
                        output.write('C: ' + str(c_best) + "\n")
                        output.write('gamma: ' + str(gamma_best) + "\n")
                        output.write('balanced: ' + str(bal_best) + "\n")
                        output.write('linear: ' + str(lin_best) + "\n")

                    if bal_best :
                        curr_class_weight='balanced'
                    else:
                        curr_class_weight=None

                    if lin_best :
                        classifier = svm.LinearSVC(C=c_curr, class_weight=curr_class_weight)
                    else :
                        classifier = svm.SVC(gamma=gamma_curr, C=c_curr, class_weight=curr_class_weight, decision_function_shape=dfs)  

                    #Check if PCA is enabled
                    if pcaEnabled :
                        pca = decomposition.PCA(numPCs/2)
                        pca.fit(data_train)
                        pca_train = pca.transform(data_train)
                        pca_val = pca.transform(data_val)

                        classifier.fit(pca_train, target_train)

                        #Find the accuracy on the validation data
                        if displayAcc:
                            print('PCA validation set accuracy:', classifier.score(pca_val, target_val))
                            output.write('PCA validation set accuracy:' + str(classifier.score(pca_val, target_val)) + "\n")

                            resultMatrix[resultRowIndex, resultSVMIndex] = classifier.score(pca_val, target_val)
                            resultSVMIndex = resultSVMIndex + 1

                        if displayMAE:
                            pred_val = classifier.predict(pca_val)
                            result = metrics.mean_absolute_error(target_val, pred_val)

                            #Find the accuracy on the validation data
                            print('PCA validation set MAE:', result)
                            output.write('PCA validation set MAE:' + str(result) + "\n")

                            resultMatrix[resultRowIndex, resultSVMIndex] = result
                            resultSVMIndex = resultSVMIndex + 1

                        if displayMSE:
                            pred_val = classifier.predict(pca_val)
                            result = metrics.mean_squared_error(target_val, pred_val)

                            #Find the accuracy on the validation data
                            print('PCA validation set MSE:', result)
                            output.write('PCA validation set MSE:' + str(result) + "\n")

                            resultMatrix[resultRowIndex, resultSVMIndex] = result
                            resultSVMIndex = resultSVMIndex + 1

                    #Fit the classifier to the training data
                    classifier.fit(data_train, target_train)

                    #Find the accuracy on the validation data
                    if displayAcc:
                        print('Validation set accuracy:', classifier.score(data_val, target_val))
                        output.write('Validation set accuracy:' + str(classifier.score(data_val, target_val)) + "\n")

                        resultMatrix[resultRowIndex, resultSVMIndex] = classifier.score(data_val, target_val)
                        resultSVMIndex = resultSVMIndex + 1

                    if displayMAE:
                        pred_val = classifier.predict(data_val)
                        result = metrics.mean_absolute_error(target_val, pred_val)

                        #Find the accuracy on the validation data
                        print('Validation set MAE:', result)
                        output.write('Validation set MAE:' + str(result) + "\n")

                        resultMatrix[resultRowIndex, resultSVMIndex] = result
                        resultSVMIndex = resultSVMIndex + 1

                    if displayMSE:
                        pred_val = classifier.predict(data_val)
                        result = metrics.mean_squared_error(target_val, pred_val)

                        #Find the accuracy on the validation data
                        print('Validation set MSE:', result)
                        output.write('Validation set MSE:' + str(result) + "\n")
                        
                        resultMatrix[resultRowIndex, resultSVMIndex] = result
                        resultSVMIndex = resultSVMIndex + 1

                    #Print the trivial data
                    if displayAcc:
                        print("Trivial classifier accuracy: ", trivAcc)
                        output.write("Trivial classifier accuracy: " + str(trivAcc) + "\n")
                        resultMatrix[resultRowIndex, resultSVMIndex] = trivAcc
                        resultSVMIndex = resultSVMIndex + 1

                    if displayMAE:
                        print("Trivial classifier MAE: ", trivMAE)
                        output.write("Trivial classifier MAE: " + str(trivMAE) + "\n")
                        resultMatrix[resultRowIndex, resultSVMIndex] = trivMAE
                        resultSVMIndex = resultSVMIndex + 1

                    if displayMSE:
                        print("Trivial classifier MSE: ", trivMSE)
                        output.write("Trivial classifier MSE: " + str(trivMSE) + "\n")
                        resultMatrix[resultRowIndex, resultSVMIndex] = trivMSE
                        resultSVMIndex = resultSVMIndex + 1

                    output.close()

                    resultRowIndex += 1

                #Find the best KNN for this data

                if showKNN :
                    
                    #Open a file for output
                    output = open("results.txt", "a")
                    print("----------", "KNN", dataType, featureType, "----------")
                    output.write("----------" + " KNN " + dataType + " " + featureType +  " ----------\n")
                    
                    k_best = 0
                    num_best = 0
                    acc_best = -5

                    for k_curr in k_vals:
                        for numFeatures in num_vals:
                            #Find CV accuracy with current params
                            #Set up the classifier
                            classifier = neighbors.KNeighborsClassifier(n_neighbors=k_curr)

                            if featureSelectEnabled :
                                #Set up the Sequential Feature Selector
                                #Here we will use Sequential Floating Forward Selection
                                sffs = SFS(classifier, 
                                           k_features=numFeatures, 
                                           forward=True, 
                                           floating=floatingEnabled, 
                                           scoring=scoreMethod,
                                           print_progress=False,
                                           cv=cv_folds)
                                sffs = sffs.fit(data_train, target_train)

                                if printHyperParamTuning :
                                    print('CV Score:', sffs.k_score_,'k: ', k_curr,'numFeatures: ', numFeatures)
                                    output.write('CV Score: ' + str(sffs.k_score_) + ', k: ' + str(k_curr) + ', numFeatures: ' + str(numFeatures) + "\n")

                                #Test if better than current best and if yest replace vals
                                if sffs.k_score_ > acc_best:
                                    acc_best = sffs.k_score_
                                    k_best = k_curr
                                    num_best = numFeatures
                            else :
                                #Use cross validation to find Accuracy
                                emotion_scores = cross_validation.cross_val_score(classifier, data_train, target_train, scoring=scoreMethod, cv=5)

                                if printHyperParamTuning :
                                    print('CV Score: ' + str(np.mean(emotion_scores)) + ', k: ' + str(k_curr))
                                    output.write('CV Score: ' + str(np.mean(emotion_scores)) + ', k: ' + str(k_curr) + '\n')

                                if np.mean(emotion_scores) > acc_best:
                                    acc_best = np.mean(emotion_scores)
                                    k_best = k_curr

                    #Now that we have the best parameters we can create
                    #a classifier to test our validation set

                    print('k: ', k_best)
                    output.write('k: ' + str(k_best) + "\n")

                    if featureSelectEnabled or postFeatSelEnabled :

                        if printHyperParamTuning :                   
                            print('Accuracy max: ', acc_best)
                            print('k: ', k_best)
                            print('numFeatures: ', num_best)

                            output.write('Accuracy max: ' + str(acc_best) + "\n")
                            output.write('k: ' + str(k_best) + "\n")
                            output.write('numFeatures: ' + str(num_best) + "\n")
                                            
                        classifier = neighbors.KNeighborsClassifier(n_neighbors=k_best)         

                        sffs = SFS(classifier, 
                                   k_features=20, 
                                   forward=True, 
                                   floating=floatingEnabled, 
                                   scoring=scoreMethod,
                                   print_progress=False,
                                   cv=cv_folds)

                        #Select the features
                        sffs = sffs.fit(data_train, target_train)
                        data_sffs_train = sffs.transform(data_train)
                        data_sffs_val = sffs.transform(data_val)

                        #Fit the classifier to the training data
                        classifier.fit(data_sffs_train, target_train)

                        #Print the features selected
                        if printSelectedFeats :
                            print('Selected features: ', end="")
                            output.write('Selected features: ')
                            for feat in sffs.k_feature_idx_ :
                                print(currentHeader[feat], end=",")
                                output.write(currentHeader[feat] + ",")
                            print()
                            output.write("\n")

                        #Find the accuracy on the validation data
                        if displayAcc:
                            print('Feat select validation set accuracy:', classifier.score(data_sffs_val, target_val))
                            output.write('Feat select validation set accuracy:' + str(classifier.score(data_sffs_val, target_val)) + "\n")

                            resultMatrix[resultRowIndex, resultKNNIndex] = classifier.score(data_sffs_val, target_val)
                            resultKNNIndex += 1

                        if displayMAE:
                            pred_val = classifier.predict(data_sffs_val)
                            result = metrics.mean_absolute_error(target_val, pred_val)

                            #Find the accuracy on the validation data
                            print('Feat select validation set MAE:', result)
                            output.write('Feat select validation set MAE:' + str(result) + "\n")

                            resultMatrix[resultRowIndex, resultKNNIndex] = result
                            resultKNNIndex += 1

                        if displayMSE:
                            pred_val = classifier.predict(data_sffs_val)
                            result = metrics.mean_squared_error(target_val, pred_val)

                            #Find the accuracy on the validation data
                            print('Feat select validation set MSE:', result)
                            output.write('Feat select validation set MSE:' + str(result) + "\n")

                            resultMatrix[resultRowIndex, resultKNNIndex] = result
                            resultKNNIndex += 1

                    if printHyperParamTuning :
                        print('Accuracy max: ', acc_best)
                        print('k: ', k_best)

                        output.write('Accuracy max: ' + str(acc_best) + "\n")
                        output.write('k: ' + str(k_best) + "\n")
                                        
                    classifier = neighbors.KNeighborsClassifier(n_neighbors=k_best)

                    #Check if PCA is enabled
                    if pcaEnabled :
                        pca = decomposition.PCA(numPCs/2)
                        pca.fit(data_train)
                        pca_train = pca.transform(data_train)
                        pca_val = pca.transform(data_val)

                        classifier.fit(pca_train, target_train)

                        #Find the accuracy on the validation data
                        if displayAcc:
                            print('PCA validation set accuracy:', classifier.score(pca_val, target_val))
                            output.write('PCA validation set accuracy:' + str(classifier.score(pca_val, target_val)) + "\n")

                            resultMatrix[resultRowIndex, resultKNNIndex] = classifier.score(pca_val, target_val)
                            resultKNNIndex += 1

                        if displayMAE:
                            pred_val = classifier.predict(pca_val)
                            result = metrics.mean_absolute_error(target_val, pred_val)

                            #Find the accuracy on the validation data
                            print('PCA validation set MAE:', result)
                            output.write('PCA validation set MAE:' + str(result) + "\n")

                            resultMatrix[resultRowIndex, resultKNNIndex] = result
                            resultKNNIndex += 1

                        if displayMSE:
                            pred_val = classifier.predict(pca_val)
                            result = metrics.mean_squared_error(target_val, pred_val)

                            #Find the accuracy on the validation data
                            print('PCA validation set MSE:', result)
                            output.write('PCA validation set MSE:' + str(result) + "\n")

                            resultMatrix[resultRowIndex, resultKNNIndex] = result
                            resultKNNIndex += 1

                    #Fit the classifier to the training data
                    classifier.fit(data_train, target_train)

                    #Find the accuracy on the validation data
                    if displayAcc:
                        print('Validation set accuracy:', classifier.score(data_val, target_val))
                        output.write('Validation set accuracy:' + str(classifier.score(data_val, target_val)) + "\n")

                        resultMatrix[resultRowIndex, resultKNNIndex] = classifier.score(data_val, target_val)
                        resultKNNIndex += 1

                    if displayMAE:
                        pred_val = classifier.predict(data_val)
                        result = metrics.mean_absolute_error(target_val, pred_val)

                        #Find the accuracy on the validation data
                        print('Validation set MAE:', result)
                        output.write('Validation set MAE:' + str(result) + "\n")

                        resultMatrix[resultRowIndex, resultKNNIndex] = result
                        resultKNNIndex += 1

                    if displayMSE:
                        pred_val = classifier.predict(data_val)
                        result = metrics.mean_squared_error(target_val, pred_val)

                        #Find the accuracy on the validation data
                        print('Validation set MSE:', result)
                        output.write('Validation set MSE:' + str(result) + "\n")

                        resultMatrix[resultRowIndex, resultKNNIndex] = result
                        resultKNNIndex += 1

                    #Print the trivial data
                    if displayAcc:
                        print("Trivial classifier accuracy: ", trivAcc)
                        output.write("Trivial classifier accuracy: " + str(trivAcc) + "\n")

                        resultMatrix[resultRowIndex, resultKNNIndex] = trivAcc
                        resultKNNIndex += 1

                    if displayMAE:
                        print("Trivial classifier MAE: ", trivMAE)
                        output.write("Trivial classifier MAE: " + str(trivMAE) + "\n")

                        resultMatrix[resultRowIndex, resultKNNIndex] = trivMAE
                        resultKNNIndex += 1

                    if displayMSE:
                        print("Trivial classifier MSE: ", trivMSE)
                        output.write("Trivial classifier MSE: " + str(trivMSE) + "\n")

                        resultMatrix[resultRowIndex, resultKNNIndex] = trivMSE
                        resultKNNIndex += 1

                    output.close()

                    resultRowIndex -= 1

            resultRowIndex += 2

np.savetxt("clfResults.csv", resultMatrix, delimiter=",")
