% load data
processed = readtable("processed.csv", 'Delimiter',';')

% get file handler
fileID = fopen("distribution.csv","w")

arrayfun(@(x) magic(x, processed, fileID, "C12"), string(unique(processed(processed.Cell == "C12", :).BSSID)),'UniformOutput',false)
arrayfun(@(x) magic(x, processed, fileID, "C13"), string(unique(processed(processed.Cell == "C13", :).BSSID)),'UniformOutput',false)
arrayfun(@(x) magic(x, processed, fileID, "C14"), string(unique(processed(processed.Cell == "C14", :).BSSID)),'UniformOutput',false)
arrayfun(@(x) magic(x, processed, fileID, "C15"), string(unique(processed(processed.Cell == "C15", :).BSSID)),'UniformOutput',false)

% close the file
fclose(fileID)
