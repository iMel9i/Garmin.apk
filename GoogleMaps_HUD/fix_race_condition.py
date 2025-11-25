#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script to fix the race condition in NotificationMonitor.java
This removes the early exit when sHud is null and uses a default queue size instead.
"""

import sys
import os

def fix_oncreate_race_condition(file_path):
    """Fix the onCreate() race condition in NotificationMonitor.java"""
    
    # Read the file
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    # Find and fix the onCreate method (around lines 143-156)
    modified = False
    i = 0
    while i < len(lines):
        # Look for the onCreate method signature
        if '@Override' in lines[i] and i + 1 < len(lines) and 'public void onCreate()' in lines[i + 1]:
            print(f"Found onCreate() at line {i + 1}")
            
            # Skip to super.onCreate()
            j = i + 2
            while j < len(lines) and 'super.onCreate()' not in lines[j]:
                j += 1
            
            if j >= len(lines):
                print("ERROR: Could not find super.onCreate()")
                return False
            
            # Now find the problematic if (null == sHud) block
            k = j + 1
            found_check = False
            while k < len(lines) and k < j + 10:  # Search within 10 lines
                if 'if (null == sHud)' in lines[k] or 'if (sHud == null)' in lines[k]:
                    found_check = True
                    check_line = k
                    # Find the closing brace of this if block
                    brace_count = 0
                    m = k
                    while m < len(lines):
                        if '{' in lines[m]:
                            brace_count += 1
                        if '}' in lines[m]:
                            brace_count -= 1
                            if brace_count == 0:
                                # Found the end of if block
                                end_if_line = m
                                break
                        m += 1
                    
                    if brace_count == 0:
                        # Remove lines from check_line to end_if_line (inclusive)
                        print(f"Removing problematic if block from line {check_line + 1} to {end_if_line + 1}")
                        del lines[check_line:end_if_line + 1]
                        
                        # Now find the maxQueueSize line and replace it
                        # It should be right after where we deleted
                        n = check_line
                        while n < len(lines) and n < check_line + 10:
                            if 'final int maxQueueSize' in lines[n] and 'sHud.getMaxUpdatesPerSecond()' in lines[n]:
                                print(f"Fixing maxQueueSize at line {n + 1}")
                                # Replace with conditional version
                                indent = len(lines[n]) - len(lines[n].lstrip())
                                lines[n] = ' ' * indent + '// CRITICAL FIX: Use default queue size if sHud is not yet initialized\n'
                                lines.insert(n + 1, ' ' * indent + 'final int maxQueueSize = (null != sHud) ? sHud.getMaxUpdatesPerSecond() : 10;\n')
                                modified = True
                                break
                            n += 1
                        break
                k += 1
            
            if not found_check:
                print("WARNING: Could not find the sHud null check")
            
            break
        i += 1
    
    if not modified:
        print("ERROR: Could not apply the fix")
        return False
    
    # Write the modified file
    with open(file_path, 'w', encoding='utf-8') as f:
        f.writelines(lines)
    
    print(f"Successfully fixed {file_path}")
    return True

if __name__ == '__main__':
    file_path = r'C:\Users\mts88\Documents\GHUD\Garmin.apk\GoogleMaps_HUD\gmaps_hud\src\main\java\sky4s\garminhud\app\NotificationMonitor.java'
    
    if not os.path.exists(file_path):
        print(f"ERROR: File not found: {file_path}")
        sys.exit(1)
    
    # Create backup
    backup_path = file_path + '.backup'
    import shutil
    shutil.copy2(file_path, backup_path)
    print(f"Created backup: {backup_path}")
    
    if fix_oncreate_race_condition(file_path):
        print("\n✓ Fix applied successfully!")
        sys.exit(0)
    else:
        print("\n✗ Fix failed, restoring backup...")
        shutil.copy2(backup_path, file_path)
        sys.exit(1)
