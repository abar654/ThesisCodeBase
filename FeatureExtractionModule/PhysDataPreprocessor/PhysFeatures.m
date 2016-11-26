clear;

%Read in the timestamps of the rating files
%This should be from a file which was created when the SmartPhone data
%processor was run.
%Loop through these calculating the features with the correct
%associated data (using the sampling rate knowledge).
%ratingTimes = csvread('rateTimes.csv')/1000;
%ratingTimes = csvread('reviewTimes.csv')/1000;
ratingTimes = csvread('combinedTimes.csv')/1000;
numSessions = 16; %The number of E4 file sets.

%For: TEMP, EDA, BVP (Absolute value), HR, ACC (root square magnitude)
files = ['TEMP'; 'EDA '; 'BVP '; 'HR  '; 'ACC '];
cellFiles = cellstr(files);

%Create a matrix to hold the results - one row for each rate time, col per feature
numFeatures = size(cellFiles,1) * 3 * 9 + 2; %This number currently only contains the features computed in the loop
                                             %Added 2 for the temperature
                                             %features
features = ones(size(ratingTimes,1), numFeatures)*-1;

%For each recording session i.e. set of data files from the E4 we want to
%process its data and add this to the correct rating time.
for session = 0:1:(numSessions-1)
    
    for fileNum = 1:1:size(cellFiles,1)
        
        noExtFileName = char(cellFiles(fileNum));
        currentFileName = strcat('e4Data/', char(cellFiles(fileNum)), '.csv');
        
        if(session ~= 0) 
            currentFileName = strcat('e4Data/',noExtFileName,'_',num2str(session),'.csv')      
        end

        if strcmp(noExtFileName,'ACC')

            %Create the accelerometer data vector
            %This requires reading a csv with 3 columns
            %then taking the root square sum of them
            header = csvread(currentFileName,0,0,[0 0 1 2]);
            startTime = header(1,1);
            sampleRate = header(2,1);

            %Read signal
            accData = csvread(currentFileName,2,0);

            %Create the data vector
            data = zeros(size(accData,1),1);

            for i = 1:size(accData,1)

                data(i) = sqrt(accData(i,1)^2 + accData(i,2)^2 + accData(i,3)^2);

            end

        else

            %Read the data files in
            %Read header info
            header = csvread(currentFileName,0,0,[0 0 1 0]);
            startTime = header(1);
            sampleRate = header(2);

            %Read signal
            data = csvread(currentFileName,2,0);
            %Take absolute value for BVP
            if strcmp(noExtFileName,'BVP')
                data = abs(data);
            end

        end

        %figure('name', char(cellFiles(fileNum)));
        %plot(data);

        %Running median filter to remove noisy spikes.
        filtData = medfilt1(data,9);
        %hold on;
        %plot(filtData, 'color', 'red');
        %hold off;

        %Global max and mean
        globalMax = max(filtData);
        globalMin = min(filtData);

        %Keep track of which sample we are up to
        sampleCount = 1;
        %Go through each labelled segment
        for i = 1:1:size(ratingTimes,1)

            %Only include data that is up to 3 hours before the ratingTime
            MAX_DATA_AGE = 3*60*60;
            while(startTime + sampleCount/sampleRate + MAX_DATA_AGE < ratingTimes(i))
                sampleCount = sampleCount + 1;
            end

            %The current segment starts at and includes this index
            startIndex = sampleCount;

            while(startTime + sampleCount/sampleRate < ratingTimes(i))
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

                    diffHist = zeros(5);
                end
                
                %If this is the skin temperature then scan for sections
                %which are below the reasonable threshold let's say 26.
                if strcmp(noExtFileName,'TEMP')
                    
                    numBelow = 0;
                    durationBelow = 0;
                    
                    for i = 2:1:size(rawData,1)
                        
                        if(rawData(i) < 26) 
                            
                            durationBelow = durationBelow + 1;
                            if(rawData(i-1) > 26)
                                numBelow = numBelow + 1
                            end
                            
                        end
       
                    end
                    
                    %Save both numBelow and durationBelow/size(rawData,1)
                    features(i, numFeatures-1) = numBelow;
                    features(i, numFeatures) = durationBelow/size(rawData,1);
                    
                end

                %Save the features in the correct part of the features matrix
                offset = (fileNum - 1) * 3*9;

                features(i, 1 + offset) = rawMean;
                features(i, 2 + offset) = rawMax;
                features(i, 3 + offset) = rawMin;
                features(i, 4 + offset) = rawVar;
                features(i, (5 + offset):(9 + offset)) = rawHist;

                offset = (fileNum - 1) * 3*9 + 9;

                features(i, 1 + offset) = normMean;
                features(i, 2 + offset) = normMax;
                features(i, 3 + offset) = normMin;
                features(i, 4 + offset) = normVar;
                features(i, (5 + offset):(9 + offset)) = normHist;

                offset = (fileNum - 1) * 3*9 + 18;

                features(i, 1 + offset) = diffMean;
                features(i, 2 + offset) = diffMax;
                features(i, 3 + offset) = diffMin;
                features(i, 4 + offset) = diffVar;
                features(i, (5 + offset):(9 + offset)) = diffHist;

            end  

        end

    end
    %Step counter for ACC

    %SCR events for EDA (using EDA-Explorer, maybe even feed EDA explorer to
    %smartphone data feature creation)
    %
    %Weight EDA based on ACC magnitude

end

%Print out features to an output file
csvwrite('physFeatures.csv',features);

%Print csv header to another file
headerFile = fopen('physHeader.csv', 'w');

sigTypes = ['raw '; 'norm'; 'diff'];
cellSigTypes = cellstr(sigTypes);

for fileNum = 1:1:size(cellFiles,1)
        
        noExtFileName = char(cellFiles(fileNum));
        
        for sigNum = 1:1:size(cellSigTypes,1)
            
            currSigType = char(cellSigTypes(sigNum));
            fprintf(headerFile, '%s%sMean,', currSigType, noExtFileName);
            fprintf(headerFile, '%s%sMax,', currSigType, noExtFileName);
            fprintf(headerFile, '%s%sMin,', currSigType, noExtFileName);
            fprintf(headerFile, '%s%sVar,', currSigType, noExtFileName);
            fprintf(headerFile, '%s%sHist1,', currSigType, noExtFileName);
            fprintf(headerFile, '%s%sHist2,', currSigType, noExtFileName);
            fprintf(headerFile, '%s%sHist3,', currSigType, noExtFileName);
            fprintf(headerFile, '%s%sHist4,', currSigType, noExtFileName);
            fprintf(headerFile, '%s%sHist5,', currSigType, noExtFileName);
            
        end 
        
end

fprintf(headerFile, 'numBelowTempThresh,');
fprintf(headerFile, 'percentBelowTempThresh\n');

fclose(headerFile);

%Print to command line for debugging
% for i = 1:size(features,1)
%     
%     for fileNum = 1:1:size(cellFiles,1)
%         
%         fprintf('%s:',char(cellFiles(fileNum)));
%         for j = ((fileNum-1)*3*9 + 1):((fileNum)*3*9)
%             fprintf('%f,', features(i,j));
%         end
%         fprintf('\n');
%         
%     end
%     
%     fprintf('\n');
%     
% end
    