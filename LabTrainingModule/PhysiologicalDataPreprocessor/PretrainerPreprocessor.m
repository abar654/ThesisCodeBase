clear;

%Read in the timestamps from viewTimes.txt
%Keep these in milliseconds
%These times denote the start of a viewing of an image
%Note also that the "finished" events are left in so any event with
%no more action for 5 minutes afterwards can be considered a "finished"
viewTimes = csvread('viewTimesOnly.csv');

numSessions = 7; %The number of E4 file sets.

%For: TEMP, EDA, BVP (Absolute value), HR, (Exclude ACC because we should
%just be sitting still in a chair for pre-training)

files = ['TEMP'; 'EDA '; 'BVP '; 'HR  '];
cellFiles = cellstr(files);

%Figure out how many images have been shown
%This should match the number of ratings in the pretrainerRatings.csv file
numImages = ceil(size(viewTimes,1)/46) * 45

%Create a matrix to hold the results - one row for each rate time, col per feature
numFeatures = size(cellFiles,1) * 3 * 9; %This number currently only contains the features computed in the loop

features = ones(numImages, numFeatures)*-1;

%For each recording session i.e. set of data files from the E4 we want to
%process its data and add this to the correct rating time.
for session = 0:1:(numSessions-1)
    for fileNum = 1:1:size(cellFiles,1)
        
        noExtFileName = char(cellFiles(fileNum));
        currentFileName = strcat(char(cellFiles(fileNum)), '.csv');
        
        if(session ~= 0) 
            currentFileName = strcat(noExtFileName,'_',num2str(session),'.csv')      
        end
        
        %Read the data files in
        %Read header info
        header = csvread(currentFileName,0,0,[0 0 1 0]);
        startTime = header(1) * 1000; %In Milliseconds
        sampleRate = header(2); %In Hz

        %Read signal
        data = csvread(currentFileName,2,0);
        %Take absolute value for BVP
        if strcmp(noExtFileName,'BVP')
            data = abs(data);
        end
        
        %Running median filter to remove noisy spikes.
        filtData = medfilt1(data,9);

        %Global max and mean
        globalMax = max(filtData);
        globalMin = min(filtData);
        
        %Keep track of which sample we are up to
        sampleCount = 1;
        numFins = 0;
               
        %Go through each labelled segment
        for i = 1:1:(size(viewTimes,1)-1)

            currTime = viewTimes(i);
            nextTime = viewTimes(i+1);
            
            %Make sure this isn't a "finished" timestamp
            if nextTime - currTime < 1000*60*5

                %Only include data that is between currTime and nextTime
                %Get the sample count up to the current time
                while(startTime + 1000*sampleCount/sampleRate < currTime)
                    sampleCount = sampleCount + 1;
                end

                %The current segment starts at and includes this index
                startIndex = sampleCount;

                while(startTime + 1000*sampleCount/sampleRate < nextTime)
                    sampleCount = sampleCount + 1;        
                end

                %The current segment finishes at and includes this index
                finishIndex = sampleCount - 1;

                %Make sure that we haven't run out of samples        
                if(finishIndex < startIndex || startIndex > size(filtData,1)) 

                    %We have no samples in this segment

                else

                    if(finishIndex > size(filtData,1))
                        finishIndex = size(filtData,1);
                    end

                    %Take 3 signal types: Raw, Normalised, Derivative (First
                    %difference?)
                    %For each signal compute the following features:
                    %Mean, min, max, variance, histogram for values (5 bins)
                    %(For EDA weight this based on ACC?)

                    %Raw
                    %Create a sub array for the samples in this segment
                    rawData = filtData(startIndex:finishIndex);

                    rawMean = mean(rawData);
                    rawMax = max(rawData);
                    rawMin = min(rawData);
                    rawVar = var(rawData);

                    del = globalMax - globalMin;
                    rawBins = [globalMin, globalMin + del/4, globalMin + 2*del/4, globalMin + 3*del/4, globalMax];
                    rawHist = hist(rawData, rawBins);
                    rawHist = rawHist/size(rawData,1);

                    %Normalised
                    normData = (rawData - rawMean)/(rawMax - rawMin);

                    normMean = mean(normData);
                    normMax = max(normData);
                    normMin = min(normData);
                    normVar = var(normData);

                    normHist = hist(normData,5)/size(normData,1);

                    %Derivative
                    if(size(rawData,1) > 1)
                        diffData = diff(rawData);

                        diffMean = mean(diffData);
                        diffMax = max(diffData);
                        diffMin = min(diffData);
                        diffVar = var(diffData);

                        diffHist = hist(diffData,5)/size(diffData,1);
                    else

                        diffMean = 0;
                        diffMax = 0;
                        diffMin = 0;
                        diffVar = 0;

                        diffHist = zeros(1, 5);
                    end

                    if (rawMax - rawMin) > 0
                        %Save the features in the correct part of the features matrix
                        offset = (fileNum - 1) * 3*9;

                        features(i-numFins, 1 + offset) = rawMean;
                        features(i-numFins, 2 + offset) = rawMax;
                        features(i-numFins, 3 + offset) = rawMin;
                        features(i-numFins, 4 + offset) = rawVar;
                        features(i-numFins, (5 + offset):(9 + offset)) = rawHist;

                        offset = (fileNum - 1) * 3*9 + 9;

                        features(i-numFins, 1 + offset) = normMean;
                        features(i-numFins, 2 + offset) = normMax;
                        features(i-numFins, 3 + offset) = normMin;
                        features(i-numFins, 4 + offset) = normVar;
                        features(i-numFins, (5 + offset):(9 + offset)) = normHist;

                        offset = (fileNum - 1) * 3*9 + 18;

                        features(i-numFins, 1 + offset) = diffMean;
                        features(i-numFins, 2 + offset) = diffMax;
                        features(i-numFins, 3 + offset) = diffMin;
                        features(i-numFins, 4 + offset) = diffVar;
                        features(i-numFins, (5 + offset):(9 + offset)) = diffHist;
                    else
                        %Save the features in the correct part of the features matrix
                        offset = (fileNum - 1) * 3*9;

                        features(i-numFins, 1 + offset) = rawMean;
                        features(i-numFins, 2 + offset) = rawMax;
                        features(i-numFins, 3 + offset) = rawMin;
                        features(i-numFins, 4 + offset) = rawVar;
                        features(i-numFins, (5 + offset):(9 + offset)) = rawHist;

                        offset = (fileNum - 1) * 3*9 + 9;

                        features(i-numFins, 1 + offset) = 0;
                        features(i-numFins, 2 + offset) = 0;
                        features(i-numFins, 3 + offset) = 0;
                        features(i-numFins, 4 + offset) = 0;
                        features(i-numFins, (5 + offset):(9 + offset)) = zeros(1, 5);

                        offset = (fileNum - 1) * 3*9 + 18;

                        features(i-numFins, 1 + offset) = 0;
                        features(i-numFins, 2 + offset) = 0;
                        features(i-numFins, 3 + offset) = 0;
                        features(i-numFins, 4 + offset) = 0;
                        features(i-numFins, (5 + offset):(9 + offset)) = zeros(1, 5);
                        
                    end

                end
                
            else
                numFins = numFins + 1;
            end
            
        end
        
    end
    
end

%Print out features to an output file
csvwrite('pretrainerFeatures.csv',features);
        
        