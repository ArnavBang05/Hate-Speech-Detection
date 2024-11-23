import pandas as pd
import numpy as np
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.model_selection import train_test_split
from sklearn.tree import DecisionTreeClassifier
from sklearn.metrics import classification_report
from abc import ABC, abstractmethod
import pickle
import re
import string
import nltk
from nltk.stem import SnowballStemmer
from nltk.corpus import stopwords
from flask import Flask, request, jsonify

nltk.download('stopwords')


# Encapsulation with Preprocessor Class
class TextPreprocessor:
    def __init__(self):
        self.__stemmer = SnowballStemmer("english")
        self.__stopwords = set(stopwords.words("english"))

    def __clean_text(self, text):
        text = str(text).lower()
        text = re.sub(r'\[.*?\]', '', text)
        text = re.sub(r'https?://\S+|www\.\S+', '', text)
        text = re.sub(r'<.*?>+', '', text)
        text = re.sub(r'[%s]' % re.escape(string.punctuation), '', text)
        text = re.sub(r'\n', '', text)
        text = re.sub(r'\w*\d\w*', '', text)
        text = [word for word in text.split() if word not in self.__stopwords]
        text = " ".join(text)
        text = [self.__stemmer.stem(word) for word in text.split()]
        return " ".join(text)

    def clean(self, text):
        return self.__clean_text(text)


# Abstract Base Class (Abstraction)
class BaseModel(ABC):
    @abstractmethod
    def train_model(self, X_train, y_train):
        pass

    @abstractmethod
    def predict(self, text):
        pass


# Inheritance and Polymorphism
class HateSpeechModel(BaseModel):
    def __init__(self, data_path):
        self.data_path = data_path
        self.preprocessor = TextPreprocessor()
        self.model = None
        self.vectorizer = None

    def load_and_prepare_data(self):
        df = pd.read_csv(self.data_path)
        if 'class' not in df.columns:
            raise KeyError("The column 'class' is missing from the dataset. Please check the dataset format.")
        df['labels'] = df['class'].map({0: "Hate Speech Detected", 1: "Offensive Language Detected", 2: "No Hate or Offensive Speech"})
        df = df[['tweet', 'labels']]
        df["tweet"] = df["tweet"].apply(self.preprocessor.clean)
        x = np.array(df["tweet"])
        y = np.array(df["labels"])
        self.vectorizer = CountVectorizer()
        x = self.vectorizer.fit_transform(x)
        return train_test_split(x, y, test_size=0.33, random_state=42)

    def train_model(self, X_train, y_train):
        self.model = DecisionTreeClassifier()
        self.model.fit(X_train, y_train)

    def save_model(self, model_path, vectorizer_path):
        with open(model_path, 'wb') as model_file:
            pickle.dump(self.model, model_file)
        with open(vectorizer_path, 'wb') as vectorizer_file:
            pickle.dump(self.vectorizer, vectorizer_file)

    def load_model(self, model_path, vectorizer_path):
        with open(model_path, 'rb') as model_file:
            self.model = pickle.load(model_file)
        with open(vectorizer_path, 'rb') as vectorizer_file:
            self.vectorizer = pickle.load(vectorizer_file)

    # Polymorphism (overriding predict)
    def predict(self, text):
        cleaned_text = self.preprocessor.clean(text)
        transformed_text = self.vectorizer.transform([cleaned_text]).toarray()
        return self.model.predict(transformed_text)


# Flask Application
app = Flask(__name__)
model = HateSpeechModel("twitter_data.csv")

# Prepare and train model
X_train, X_test, y_train, y_test = model.load_and_prepare_data()
model.train_model(X_train, y_train)
model.save_model("model.pkl", "cv.pkl")


@app.route('/analyze', methods=['POST'])
def analyze():
    data = request.get_json()
    message = data.get("text", "")

    if not message:
        return jsonify({"error": "No text provided for analysis."}), 400

    prediction = model.predict(message)

    if prediction[0] == "Hate Speech Detected":
        result = {"hate_speech": True, "message": "Hate Speech Detected"}
    elif prediction[0] == "Offensive Language Detected":
        result = {"hate_speech": False, "message": "Offensive Language Detected"}
    else:
        result = {"hate_speech": False, "message": "Message is clean"}

    return jsonify(result)


if __name__ == '__main__':
    app.run(debug=True)
