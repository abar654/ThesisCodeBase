clear;

%Read in the timestamps of the rating files
%This should be from a file which was created when the SmartPhone data
%processor was run.
%Loop through these calculating the features with the correct
%associated data (using the sampling rate knowledge).
%ratingTimes = csvread('rateTimes.csv')/1000;
%ratingTimes = csvread('reviewTimes.csv')/1000;
ratingTimes = csvread('combinedTimes.csv')/1000;

%Constants
numSessions = 29; %The number of E4 file sets.
MAX_DATA_AGE = 3*60*60;

%Open the output file where results will be written
outputFile = fopen('windowedFeatures.csv', 'w');

%For each recording session i.e. set of data files from the E4 we want to
%process its data and add this to the correct rating time.
for session = 0:1:(numSessions-1)
    
    %Prepare all the file names
    currentTempFile = 'e4Data/TEMP';
    currentEdaFile = 'e4Data/EDA';
    currentBvpFile = 'e4Data/BVP';
    currentHrFile = 'e4Data/HR';
    
    if(session ~= 0) 
        currentTempFile = strcat(currentTempFile,'_',num2str(session));
        currentEdaFile = strcat(currentEdaFile,'_',num2str(session));
        currentBvpFile = strcat(currentBvpFile,'_',num2str(session));
        currentHrFile = strcat(currentHrFile,'_',num2str(session));
    end
    
    currentTempFile = strcat(currentTempFile,'.csv')
    currentEdaFile = strcat(currentEdaFile,'.csv');
    currentBvpFile = strcat(currentBvpFile,'.csv');
    currentHrFile = strcat(currentHrFile,'.csv');
    
    %Read the data files in
    %Read header info
    header = csvread(currentTempFile,0,0,[0 0 1 0]);
    
    %Set currentTime to be the seconds of the start time
    startTime = header(1);
    currentTime = startTime;
    
    %Set all the sample rates
    tempSR = header(2);
    header = csvread(currentEdaFile,0,0,[0 0 1 0]);
    edaSR = header(2);
    header = csvread(currentBvpFile,0,0,[0 0 1 0]);
    bvpSR = header(2);
    header = csvread(currentHrFile,0,0,[0 0 1 0]);
    hrSR = header(2);
    
    allSRs = [tempSR, edaSR, bvpSR, hrSR];
    
    %Read in all the signals
    %Also apply a median filter
    tempData = medfilt1(csvread(currentTempFile,2,0),9);
    edaData = medfilt1(csvread(currentEdaFile,2,0),9);
    bvpData = medfilt1(abs(csvread(currentBvpFile,2,0)),9);
    hrData = medfilt1(csvread(currentHrFile,2,0),9);
    
    allData = {tempData; edaData; bvpData; hrData};
    
    %Get global mins and maxs
    globalMax = [max(tempData), max(edaData), max(bvpData), max(hrData)];
    globalMin = [min(tempData), min(edaData), min(bvpData), min(hrData)];
    
    %End time for this session (not including this second)
    endTime = currentTime + size(hrData, 1)/hrSR;
    
    %Until you are out of samples or out of labels (in which case we break)
    while (1)
        
        %Find the first label that is after currentTime
        currLabelTime = 0;
        for i = 1:1:size(ratingTimes,1)
            if (ratingTimes(i) > currentTime)
                currLabelTime =ratingTimes(i)
                break;
            end
        end
        
        if currLabelTime == 0 
            break;
        end
        
        %If this label is more than 3 hours away then
        %advance the currentTime until you are either 3 hours away or at
        %the endTime.
        if currentTime + MAX_DATA_AGE < currLabelTime
            
            currentTime = currLabelTime - MAX_DATA_AGE;
            if currentTime >= endTime
                break;
            end

        end
       
        %If not at the end, take the next 10s block (end if not 10s left) and compute the features.
        %Keep doing this while there are samples and you are before the labelTime
        while(currentTime + 10 < currLabelTime && currentTime + 10 < endTime)
            
            %Write features for this 10s period on one line which is comma seperated with the first
            %element being the currentLabelTime
            fprintf(outputFile, '%f', currLabelTime);
            
            %Write the features file by file
            for k = 1:1:4
                
                %Take 3 signal types: Raw, Normalised, Derivative 
                %For each signal compute the following features:
                %Mean, min, max, variance, histogram for values (5 bins)

                %Raw
                %Create a sub array for the samples in this segment
                startIndex = (currentTime - startTime)*allSRs(k) + 1;
                finishIndex = startIndex + 10*allSRs(k);
                rawData = allData{k}(startIndex:finishIndex);

                rawMean = mean(rawData);
                rawMax = max(rawData);
                rawMin = min(rawData);
                rawVar = var(rawData);

                del = globalMax(k) - globalMin(k);
                rawBins = [globalMin(k), globalMin(k) + del/4, globalMin(k) + 2*del/4, globalMin(k) + 3*del/4, globalMax(k)];
                rawHist = hist(rawData, rawBins);
                rawHist = rawHist/size(rawData,1);

                %Normalised
                %Derivative
                if(rawMax - rawMin) > 0
                    
                    normData = (rawData - rawMean)/(rawMax - rawMin);

                    normMean = mean(normData);
                    normMax = max(normData);
                    normMin = min(normData);
                    normVar = var(normData);

                    normHist = hist(normData,5)/size(normData,1);
                    
                    diffData = diff(rawData);

                    diffMean = mean(diffData);
                    diffMax = max(diffData);
                    diffMin = min(diffData);
                    diffVar = var(diffData);

                    diffHist = hist(diffData,5)/size(diffData,1);
                else
                    
                    normMean = 0;
                    normMax = 0;
                    normMin = 0;
                    normVar = 0;

                    normHist = zeros(1, 5);

                    diffMean = 0;
                    diffMax = 0;
                    diffMin = 0;
                    diffVar = 0;

                    diffHist = zeros(1, 5);
                end
                
                %Print the features out
                fprintf(outputFile, ',%f', rawMean);
                fprintf(outputFile, ',%f', rawMax);
                fprintf(outputFile, ',%f', rawMin);
                fprintf(outputFile, ',%f', rawVar);
                fprintf(outputFile, ',%f,%f,%f,%f,%f', rawHist(1), rawHist(2), rawHist(3), rawHist(4), rawHist(5));
                
                fprintf(outputFile, ',%f', normMean);
                fprintf(outputFile, ',%f', normMax);
                fprintf(outputFile, ',%f', normMin);
                fprintf(outputFile, ',%f', normVar);
                fprintf(outputFile, ',%f,%f,%f,%f,%f', normHist(1), normHist(2), normHist(3), normHist(4), normHist(5));
                
                fprintf(outputFile, ',%f', diffMean);
                fprintf(outputFile, ',%f', diffMax);
                fprintf(outputFile, ',%f', diffMin);
                fprintf(outputFile, ',%f', diffVar);
                fprintf(outputFile, ',%f,%f,%f,%f,%f', diffHist(1), diffHist(2), diffHist(3), diffHist(4), diffHist(5));
                
            end
            
            fprintf(outputFile, '\n');
            
            currentTime = currentTime + 10;
            
        end
        
        currentTime = currentTime + 10;
        
        if(currentTime + 10 >= endTime)
                break;
        end   
    
    end

end

fclose(outputFile);

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
    