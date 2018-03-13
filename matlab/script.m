% load data
processed = readtable("processed.csv")

% get file handler
fileID = fopen("distribution.csv","w")

arrayfun(@(x) magic(x, processed, fileID, "C12"), string(unique(processed(processed.Cell == "C12", :).BSSID)),'UniformOutput',false)

% close the file
fclose(fileID)
