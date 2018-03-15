function dist = magic(bssid, M, filehandler, c)
  try
    dist = fitdist(M((M.Cell == c) & (M.BSSID == bssid) & (M.Direction == "D1"),:).Strength, 'normal')
    fprintf(filehandler,'%3s,%17s,%3.5f,%2.5f\n',c, bssid, dist.mu, dist.sigma);
  catch me
    dist = 1
  end
end
