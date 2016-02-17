#! /bin/env Rscript
library(data.table,quietly=T,warn.conflicts=F)
library(dplyr,quietly=T,warn.conflicts=F)

x = list.files(".") %>% 
  as.list %>% 
  lapply(function(file) { 
    fread(file,sep=" ") %>% 
    mutate(set=file)
    }) %>% 
  rbindlist %>%
  group_by(set) %>%
  mutate(setTime = sum(V2+V3)) %>%
  ungroup() %>%
  filter(V1 == 29) %>% 
  select(V6, setTime)

#sprintf("MeanFitness sdFitness meanTime sdTime directory\n") %>% cat
sprintf("%f %f %f %f %s\n",
  mean(x$V6),
  sd(x$V6),
  mean(x$setTime),
  sd(x$setTime),
  (getwd() %>% strsplit("/") %>% unlist %>% tail(1))
  ) %>% cat
