
close all
clear
clc
     
%Definir les variables
k = 40;% nombre de classes
n = 10;% nombre d'images par classe
     

ntraining=8; %nb d'images par classe dans training set 
ntest=2;     %nb d'images par classe dans test set 

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Analyse en composantes principales (ACP)
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%extraire la matrice contenant les images de training set
display('récupération des données en cours...')

FaceDatabase=imageSet('att_faces_2','recursive');
f=read(FaceDatabase(1),2);
%%
%FaceDatabase=imageSet('att_faces_2','recursive');

%deviser l'ensemble en 2 sous ensembles :training set et test set
[training,test] = partition(FaceDatabase,[ntraining/n ntest/n]);

face=[];
for i = 1:k
    for j=1:ntraining
        face=cat(3,face,read(training(i),j));
        label{(i-1)*ntraining+j}=int2str(i);
    end
    
end


% lire image dans la matrice T
[nRow,nCol,M] = size(face);

%T est la matrice contenant les vecteurs pour chaque image
T = reshape(face,[nRow*nCol M]);
T=im2double(T);


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%extraire la matrice contenant les images de Test set
face=[];
for i = 1:k
    for j=1:ntest
        face=cat(3,face,read(test(i),j));
    end
    
end

[nRow,nCol,MM] = size(face);

%Tes est la matrice contenant les vecteurs pour chaque image
Tes = reshape(face,[nRow*nCol MM]);
Tes=im2double(Tes);


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% mTot est la moyenne de tous le training set
mTot = mean(T,2);

% soustaire la moyenne
A = T-repmat(mTot,1,M);

% obtenir les valeurs et vecteurs propores de A'A
[V,D] = eig(A'*A);

% ordonner les valeurs propre dans l'ordre croissant
eval = diag(D);

peval = [];
pevec = [];
for i = M:-1:k+1 
    peval = [peval eval(i)];
    pevec = [pevec V(:,i)];
end

% obtenir les vecteurs propores
U = A * pevec; 

% coordonées des images dans le nouvel espace 
 Wpca = U'*A;
display('les données on été récupérées avec succés :)')

 %%
%%appliquer le classifieur SVM multi classes au training set

display('Veuillez patienter pendant que les SVMs s entrainent.')
faceClassifier = fitcecoc(Wpca',label);
display('l entraienement des SVMs terminé avec succés :)')

%%
% calcul le taux du succes 
% parfor c'est pour le calcul paralléle de la boucle for
cpt=0;
display('le calcul du taux de succes en cours...')
tic
parfor i = 1:k*ntest
   Tr = reshape(Tes(:,i),[nRow*nCol 1]);
    Ar = Tr-mTot;
    Wrec = U'*Ar;
    [pred]= predict(faceClassifier,Wrec');
    if str2double(pred)==ceil(i/ntest)
        cpt=cpt+1;
    end
end
toc
tauxSucces=cpt/(k*ntest);
sprintf('le taux du succes est : %f %',tauxSucces*100)

%%

numTest= input('veuillez choisir le numéro de l image ');
Tr = reshape(Tes(:,numTest),[nRow*nCol 1]);
Ar = Tr-mTot;
Wrec = U'*Ar;
%%faire la prédiction de la classe de la requéte
[pred]= predict(faceClassifier,Wrec');

%%afficher les résultats
if mod(numTest,ntest)==0
    index=ntest;
else
index=mod(numTest,ntest);
end

subplot(1,3,1);imshow(read(test(ceil(numTest/ntest)),index));title('votre requéte');
subplot(1,3,2);imshow(read(training(str2double(pred)),1));title('meilleur visage trouvé');
subplot(1,3,3);montage(FaceDatabase(str2double(pred)).ImageLocation);title('ensemble des images ');
display('Terminé avec succés :)')
