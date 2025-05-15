import argparse
import speech_recognition as sr
from googletrans import Translator
from gtts import gTTS
import pygame
import uuid
import os
import sys
import io

# Set default encoding to UTF-8 for terminal output
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

def recognize_speech(lang_code):
    recognizer = sr.Recognizer()
    with sr.Microphone(device_index=1) as source:
        recognizer.pause_threshold = 2.0
        print(f"Speak now ({lang_code})...")
        try:
            audio = recognizer.listen(source, timeout=10, phrase_time_limit=20)
            text = recognizer.recognize_google(audio, language=lang_code)
            print(f"You said: {text}")
            return text
        except sr.UnknownValueError:
            print("Could not understand the audio.")
        except sr.RequestError as e:
            print(f"Could not request results: {e}")
        return None

def translate_text(text, target_lang):
    translator = Translator()
    translated = translator.translate(text, dest=target_lang)
    print(f"Translated: {translated.text}")
    return translated.text

def speak_text(text, lang='en'):
    tts = gTTS(text=text, lang=lang)
    filename = f"temp_{uuid.uuid4().hex}.mp3"
    tts.save(filename)

    pygame.mixer.init()
    pygame.mixer.music.load(filename)
    pygame.mixer.music.play()

    while pygame.mixer.music.get_busy():
        pygame.time.Clock().tick(10)

    pygame.mixer.quit()
    os.remove(filename)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--text', type=str, help='Text to translate')
    parser.add_argument('--source', type=str, default='id', help='Source language (ISO code)')
    parser.add_argument('--target', type=str, default='hi', help='Target language (ISO code)')
    parser.add_argument('--no-tts', action='store_true', help='Do not use TTS')

    args = parser.parse_args()

    if args.text:
        translated = translate_text(args.text, args.target)
        if not args.no_tts:
            speak_text(translated, args.target)
        print(translated)
    else:
        text = recognize_speech(args.source)
        if text:
            translated = translate_text(text, args.target)
            if not args.no_tts:
                speak_text(translated, args.target)
            print(translated)
        else:
            print("Speech not recognized.")

if __name__ == "__main__":
    main()
