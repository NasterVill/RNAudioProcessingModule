using ReactNative.Bridge;
using System;
using System.Collections.Generic;
using Windows.ApplicationModel.Core;
using Windows.UI.Core;

namespace Audio.Processing.RNAudioProcessing
{
    /// <summary>
    /// A module that allows JS to share data.
    /// </summary>
    class RNAudioProcessingModule : NativeModuleBase
    {
        /// <summary>
        /// Instantiates the <see cref="RNAudioProcessingModule"/>.
        /// </summary>
        internal RNAudioProcessingModule()
        {

        }

        /// <summary>
        /// The name of the native module.
        /// </summary>
        public override string Name
        {
            get
            {
                return "RNAudioProcessing";
            }
        }
    }
}
