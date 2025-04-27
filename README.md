# SimpleFileReader

This is a simple Android app to **read and display PDF, DOCX, and XLSX files** from internal and external storage.

## Features

- **PDF reading**:
    
    - Uses **Android's built-in `PdfRenderer`** to render and display PDF files.
        
    - **Optimized with cache** to make PDF rendering **smoother and faster**, especially when switching pages.
        
- **DOCX and XLSX reading** (see feature branches):
    
    - Supports reading DOCX and XLSX files using third-party libraries like:
        
        - [Apache POI](https://poi.apache.org/)
            
        - [docx4j](https://www.docx4java.org/)
            
        - Other similar libraries
            
    - **Note**: These libraries only help **read the data**.  
        You must **manually visualize** the content in your app.
        
        Example for XLSX files:
        
        - Use Apache POI to access specific cell positions.
            
        - Extract the raw data.
            
        - Build your own UI.
            
        
        **For manual visualization**, you can:
        
        - Render content as **custom HTML** inside a WebView.
            
        - Or draw directly on a **Canvas** and render to a **Bitmap**.
            

## How It Works

- **PDF files**:
    
    - Open using Android's `PdfRenderer`.
        
    - Render each page as a bitmap image.
        
    - **Cache pages** smartly to reduce loading time and memory usage.
        
- **DOCX files**:
    
    - Read paragraphs, tables, and document structures via library APIs.
        
    - Visualize manually using HTML rendering or custom Bitmap drawing.
        
- **XLSX files**:
    
    - Read sheets, rows, and cells via library APIs.
        
    - Extract and map data manually to display in your own UI components.
        

## Tech Stack

- **Kotlin**
    
- **Android Jetpack**
    
- **MVVM architecture** (ViewModel, Repository)
    
- **Hilt** for Dependency Injection
    
- **Flow** for reactive streams
    
- **Storage Access Framework (SAF)** for Android 13+ support
    
- **MediaStore** for Android 12 and below
    
- **Caching** for improving PDF rendering performance
    

## Screenshot
<img src="https://github.com/user-attachments/assets/bc1fbafe-434e-4337-9d53-8aaa24a251f5" width="300">
<img src="https://github.com/user-attachments/assets/0b18e789-c171-4f7c-b40e-20a599500047" width="300">

## Note

- This project focuses on **file reading** and **basic data handling**.
    
- **PDF rendering** is optimized using caching techniques for better UX.
    
- **DOCX/XLSX visualization** must be **custom-built**:
    
    - You can either **render via HTML** inside a WebView,
        
    - Or **draw manually on Bitmap/Canvas** for full control.
        

---
