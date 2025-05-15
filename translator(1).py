import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

import speech_recognition as sr
from googletrans import Translator
from gtts import gTTS
import pygame
import uuid
import os

# Default languages (will be overridden by Java input)
source_lang = 'id'  # Indonesian
target_lang = 'hi'  # Hindi

def test_microphone():
    recognizer = sr.Recognizer()
    try:
        with sr.Microphone() as source:
            print("🎙 Testing microphone input... Please speak.", flush=True)
            recognizer.adjust_for_ambient_noise(source, duration=1)
            audio = recognizer.listen(source, timeout=5)
            print("🎧 Microphone input is working!", flush=True)
            return True
    except sr.WaitTimeoutError:
        print("❌ Microphone input timed out.", flush=True)
    except OSError as e:
        print(f"⚠ Microphone error: {e}", flush=True)
    return False

def recognize_speech(lang_code):
    recognizer = sr.Recognizer()
    with sr.Microphone() as source:
        recognizer.pause_threshold = 2.0
        print(f"🎙 Speak now ({lang_code})...", flush=True)
        try:
            audio = recognizer.listen(source, timeout=5, phrase_time_limit=8)
            text = recognizer.recognize_google(audio, language=lang_code)
            print(f"📝 You said: {text}", flush=True)
            if lang_code == source_lang:
                print(f"PHASE1 INDO RECOGNIZED: {text}", flush=True)
            else:
                print(f"PHASE2 HINDI RECOGNIZED: {text}", flush=True)
            return text
        except sr.UnknownValueError:
            print("❌ Could not understand the audio.", flush=True)
        except sr.RequestError as e:
            print(f"⚠ Could not request results from Google Speech Recognition service; {e}", flush=True)
    return None

def translate_text(text, target_lang_code):
    translator = Translator()
    try:
        translated = translator.translate(text, dest=target_lang_code)
        print(f"🌐 Translated: {translated.text}", flush=True)
        if target_lang_code == target_lang:
            print(f"PHASE1 INDO TRANSLATED: {translated.text}", flush=True)
        else:
            print(f"PHASE2 HINDI TRANSLATED: {translated.text}", flush=True)
        return translated.text
    except Exception as e:
        print(f"⚠ Translation error: {e}", flush=True)
        return None

def speak_text(text, lang='en'):
    tts = gTTS(text=text, lang=lang)
    filename = f"temp_{uuid.uuid4().hex}.mp3"
    tts.save(filename)

    pygame.mixer.init()
    pygame.mixer.music.load(filename)
    pygame.mixer.music.set_volume(1.0)
    pygame.mixer.music.play()

    while pygame.mixer.music.get_busy():
        pygame.time.Clock().tick(10)

    pygame.mixer.quit()
    os.remove(filename)

def run_translation_round(from_lang, to_lang, phase):
    text = recognize_speech(from_lang)
    if text:
        translated = translate_text(text, to_lang)
        if translated:
            speak_text(translated, lang=to_lang)

            if phase == 1:
                print(f"PHASE1 INDO RECOGNIZED: {text}", flush=True)
                print(f"PHASE1 INDO TRANSLATED: {translated}", flush=True)
            elif phase == 2:
                print(f"PHASE2 HINDI RECOGNIZED: {text}", flush=True)
                print(f"PHASE2 HINDI TRANSLATED: {translated}", flush=True)

def main():
    global source_lang, target_lang

    # Read two lines for language codes from Java
    try:
        source_lang = sys.stdin.readline().strip()
        target_lang = sys.stdin.readline().strip()
        print(f"Languages received: {source_lang} -> {target_lang}", flush=True)
    except Exception as e:
        print(f"⚠ Failed to read language codes: {e}", flush=True)
        return

    # Read next command (should be "translate")
    command = sys.stdin.readline().strip()
    if command != "translate":
        print(f"⚠ Unknown command: {command}", flush=True)
        return

    if not test_microphone():
        print("⚠ Please fix your microphone setup and try again.", flush=True)
        return

    print("\n🔁 First turn", flush=True)
    run_translation_round(source_lang, target_lang, phase=1)

    print("\n🔁 Second turn (languages swapped)", flush=True)
    run_translation_round(target_lang, source_lang, phase=2)

    print("TRANSLATION CYCLE COMPLETE", flush=True)

if __name__ == "__main__":
    main()
