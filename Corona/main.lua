--
--  main.lua
--  Rivendell Sample App
--
--  Copyright (c) 2021 Yodo1. All rights reserved.
--

local _, rivendell = pcall(require, "plugin.rivendell")

local widget = require("widget")
local json = require("json")

local appStatus = {
  customYTest = false,                -- adds UI elements to test custom Y positioning
  useAndroidImmersive = false         -- sets android ui visibility to immersiveSticky to test hidden UI bar
}


BannerAlign = {
  BANNER_LEFT = 1,
  BANNER_HORIZONTAL_CENTER = 2,
  BANNER_RIGHT = 4,
  BANNER_TOP = 8,
  BANNER_VERTICAL_CENTER = 16,
  BANNER_BOTTOM = 32
}

--------------------------------------------------------------------------
-- set up UI
--------------------------------------------------------------------------

display.setStatusBar( display.HiddenStatusBar )
display.setDefault( "background", 1 )
if appStatus.useAndroidImmersive then
  native.setProperty( "androidSystemUiVisibility", "immersiveSticky")
end


local setRed = function(self)
  self:setFillColor(1,0,0)
end

local setGreen = function(self)
  self:setFillColor(0,1,0)
end

local r1
local r2

if (appStatus.customYTest) then
  r1 = display.newRect(0,0,50,50)
  r1.anchorX, r1.anchorY = 0, 0
  setRed(r1)
  r2 = display.newRect(0,0,50,50)
  r2.anchorX, r2.anchorY = 1, 1
  setRed(r2)
end

local subTitle = display.newText {
  text = "plugin for Corona SDK",
  font = display.systemFont,
  fontSize = 14
}
subTitle:setTextColor( 0.2, 0.2, 0.2 )

eventDataTextBox = native.newTextBox( display.contentCenterX, display.contentHeight - 50, display.contentWidth - 10, 150)
eventDataTextBox.placeholder = "Event data will appear here"
eventDataTextBox.hasBackground = false

local processEventTable = function(event)
  local logString = json.prettify(event):gsub("\\","")
  logString = "\nPHASE: "..event.phase.." - - - - - - - - - \n" .. logString
  print(logString)
  eventDataTextBox.text = logString .. eventDataTextBox.text
end

-- --------------------------------------------------------------------------
-- -- plugin implementation
-- --------------------------------------------------------------------------

-- forward declarations
local appId = "n/a"
local platformName = system.getInfo("platformName")
local iReady
local bReady
local rReady
local bannerLine
local oldOrientation

if platformName == "Android" then
  appId = "emXSs0Md7ZC"
elseif platformName == "iPhone OS" then
  appId = "BkFDs2l8K4k"
else
  print "Unsupported platform"
end

print("App ID: "..appId)

local rivendellListener = function(event)
  processEventTable(event)

  if (event.phase == "loaded") then
    if (event.type == "interstitial") then
      setGreen(iReady)
    elseif (event.type == "rewardedVideo") then
      setGreen(rReady)
    elseif (event.type == "banner") then
      setGreen(bReady)
    end
  end
end

-- initialize Rivendell
if rivendell then rivendell.init(rivendellListener, appId) end

local interstitialBG = display.newRect(0,0,320,30)

local interstitialLabel = display.newText {
  text = "INTERSTITIAL",
  font = display.systemFontBold,
  fontSize = 18,
}
interstitialLabel:setTextColor(1)

local rewardedLabel = display.newText {
  text = "REWARDED",
  font = display.systemFontBold,
  fontSize = 18,
}
rewardedLabel:setTextColor(1)

local showInterstitialButton = widget.newButton {
  label = "Show Interstitial Ad",
  width = 65,
  height = 40,
  labelColor = { default={ 0, 0, 0 }, over={ 0.7, 0.7, 0.7 } },
  onRelease = function(event)
    setRed(iReady)
    rivendell.showInterstitialAd()
  end
}


local showRewardedButton = widget.newButton {
  label = "Show Rewarded Ad",
  width = 65,
  height = 40,
  labelColor = { default={ 0, 0, 0 }, over={ 0.7, 0.7, 0.7 } },
  onRelease = function(event)
    setRed(rReady)
    rivendell.showRewardedAd()
  end
}

local bannerBG = display.newRect(0,0,320,30)
bannerBG:setFillColor(1,0,0,0.7)

local bannerLabel = display.newText {
  text = "B A N N E R",
  font = display.systemFontBold,
  fontSize = 18,
}
bannerLabel:setTextColor(1)

local hideBannerButton = widget.newButton {
  label = "Hide",
  width = 100,
  height = 40,
  labelColor = { default={ 0, 0, 0 }, over={ 0.7, 0.7, 0.7 } },
  onRelease = function(event)
    rivendell.dismissBannerAd()
  end
}

local showBannerButtonT = widget.newButton {
  label = "Top",
  fontSize = 14,
  width = 100,
  height = 40,
  labelColor = { default={ 0, 0, 0 }, over={ 0.7, 0.7, 0.7 } },
  onRelease = function(event)
    rivendell.showBannerAdWithAlign(BannerAlign.BANNER_TOP)
  end
}

local showBannerButtonB = widget.newButton {
  label = "Bottom",
  fontSize = 14,
  width = 100,
  height = 40,
  labelColor = { default={ 0, 0, 0 }, over={ 0.7, 0.7, 0.7 } },
  onRelease = function(event)
    rivendell.showBannerAdWithAlign(BannerAlign.BANNER_BOTTOM)
  end
}

iReady = display.newCircle(10, 10, 6)
iReady.strokeWidth = 2
iReady:setStrokeColor(0)
setRed(iReady)

bReady = display.newCircle(10, 10, 6)
bReady.strokeWidth = 2
bReady:setStrokeColor(0)
setRed(bReady)

rReady = display.newCircle(10, 10, 6)
rReady.strokeWidth = 2
rReady:setStrokeColor(0)
setRed(rReady)

-- --------------------------------------------------------------------------
-- -- device orientation handling
-- --------------------------------------------------------------------------

local layoutDisplayObjects = function(orientation)
  if (appStatus.customYTest) then
    r1.x = display.screenOriginX
    r1.y = display.screenOriginY
    r2.x = display.actualContentWidth + display.screenOriginX
    r2.y = display.actualContentHeight + display.screenOriginY
  end

  subTitle.x = display.contentCenterX
  subTitle.y = 60

  bannerLine = display.newLine( display.screenOriginX, 72, display.actualContentWidth, 72)
  bannerLine.strokeWidth = 2
  bannerLine:setStrokeColor(1,0,0)

  if (orientation == "portrait") then
    eventDataTextBox.x = display.contentCenterX
    eventDataTextBox.y = display.contentHeight - 50
    eventDataTextBox.width = display.contentWidth - 10
  else
    -- put it waaaay offscreen
    eventDataTextBox.y = 2000
  end

  interstitialBG.x, interstitialBG.y = display.contentCenterX, 140
  interstitialBG:setFillColor(1,0,0,0.7)

  interstitialLabel.x = display.contentCenterX - 70
  interstitialLabel.y = 140

  iReady.x = display.contentCenterX - 140
  iReady.y = 140
  setRed(iReady)

  showInterstitialButton.x = display.contentCenterX - 50
  showInterstitialButton.y = interstitialLabel.y + 40

  rewardedLabel.x = display.contentCenterX + 80
  rewardedLabel.y = 140

  rReady.x = display.contentCenterX + 140
  rReady.y = 140
  setRed(rReady)

  showRewardedButton.x = display.contentCenterX + 120
  showRewardedButton.y = rewardedLabel.y + 40

  bannerBG.x, bannerBG.y = display.contentCenterX, 220

  bannerLabel.x = display.contentCenterX
  bannerLabel.y = 220

  bReady.x = display.contentCenterX + 140
  bReady.y = 220
  setRed(bReady)

  hideBannerButton.x = display.contentCenterX + 50
  hideBannerButton.y = bannerLabel.y + 40

  showBannerButtonB.x = display.contentCenterX
  showBannerButtonB.y = bannerLabel.y + 80

  showBannerButtonT.x = display.contentCenterX - 100
  showBannerButtonT.y = bannerLabel.y + 80

end

local onOrientationChange = function(event)
  local eventType = event.type
  local orientation = eventType:starts("landscape") and "landscape" or eventType

  if (orientation == "portrait") or (orientation == "landscape") then
    if (oldOrientation == nil) then
      oldOrientation = orientation
    else
      if (orientation ~= oldOrientation) then
        oldOrientation = orientation
        rivendell.hide()
        layoutDisplayObjects(eventType)
      end
    end
  end
end

Runtime:addEventListener("orientation", onOrientationChange)

-- initial layout
layoutDisplayObjects(system.orientation)
