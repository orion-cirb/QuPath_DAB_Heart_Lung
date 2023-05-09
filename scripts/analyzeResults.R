# Install and load ggplot2 package
install.packages("ggplot2")       
library("ggplot2")              

# Rename third column of the table
colnames(detailedResults)[3] ="Areas"
detailedResults <- transform(detailedResults, Areas = as.numeric(Areas))

# 1. Histogram with base R: number of bins selected manually
hist(detailedResults$Areas, breaks = 2000, xlim=c(0,50))

# Histogram with ggplot2: number of bins chosen automatically
ggplot(data=detailedResults, aes(x=Areas)) + geom_histogram(colour="white") + xlim(0, 50)

